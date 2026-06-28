import Flutter
import MediaPlayer
import CoreImage

// Extracts the dominant (average) color from an artwork natively, reusing the
// already-available MPMediaItem artwork instead of round-tripping the
// compressed bytes to Dart.
//
// Returns the ARGB color as an Int (consumed in Dart via `Color(value)`),
// or nil when the item has no artwork.
class ArtworkColorQuery {
    var args: [String: Any]
    var result: FlutterResult

    // Shared context so the GPU/Core Image setup isn't rebuilt per call.
    private static let ciContext = CIContext(options: [.workingColorSpace: NSNull()])

    init() {
        self.args = try! PluginProvider.call().arguments as! [String: Any]
        self.result = try! PluginProvider.result()
    }

    func queryArtworkColor() {
        let id = args["id"] as! Int

        var cursor: MPMediaQuery?
        var filter: MPMediaPropertyPredicate?

        let uri = args["type"] as! Int
        switch uri {
        case 0:
            filter = MPMediaPropertyPredicate(value: id, forProperty: MPMediaItemPropertyPersistentID)
            cursor = MPMediaQuery.songs()
        case 1:
            filter = MPMediaPropertyPredicate(value: id, forProperty: MPMediaItemPropertyAlbumPersistentID)
            cursor = MPMediaQuery.albums()
        case 2:
            filter = MPMediaPropertyPredicate(value: id, forProperty: MPMediaPlaylistPropertyPersistentID)
            cursor = MPMediaQuery.playlists()
        case 3:
            filter = MPMediaPropertyPredicate(value: id, forProperty: MPMediaItemPropertyArtistPersistentID)
            cursor = MPMediaQuery.artists()
        case 4:
            filter = MPMediaPropertyPredicate(value: id, forProperty: MPMediaItemPropertyGenrePersistentID)
            cursor = MPMediaQuery.genres()
        default:
            filter = nil
            cursor = nil
        }

        if cursor == nil || filter == nil {
            Log.type.warning("Cursor or filter has null value!")
            result(nil)
            return
        }

        cursor!.addFilterPredicate(filter!)

        loadColor(cursor: cursor, uri: uri)
    }

    private func loadColor(cursor: MPMediaQuery!, uri: Int) {
        DispatchQueue.global(qos: .userInitiated).async {
            var item: MPMediaItem?

            // 'uri' == 0         -> artwork is from [Song]
            // 'uri' == 1, 2 or 3 -> artwork is from [Album], [Playlist] or [Artist]
            if uri == 0 {
                item = cursor!.items?.first
            } else {
                item = cursor!.collections?.first?.items[0]
            }

            // A small size is enough to find the average color.
            let cgSize = CGSize(width: 64, height: 64)
            let image: UIImage? = item?.artwork?.image(at: cgSize)
            let color = image.flatMap { self.averageColor(of: $0) }

            DispatchQueue.main.async {
                self.result(color)
            }
        }
    }

    // One-pass average using Core Image's CIAreaAverage, packed as 0xFFRRGGBB.
    private func averageColor(of image: UIImage) -> Int? {
        guard let ciImage = CIImage(image: image) else { return nil }

        let extent = ciImage.extent
        guard let filter = CIFilter(name: "CIAreaAverage", parameters: [
            kCIInputImageKey: ciImage,
            kCIInputExtentKey: CIVector(cgRect: extent),
        ]), let output = filter.outputImage else { return nil }

        var bitmap = [UInt8](repeating: 0, count: 4)
        ArtworkColorQuery.ciContext.render(
            output,
            toBitmap: &bitmap,
            rowBytes: 4,
            bounds: CGRect(x: 0, y: 0, width: 1, height: 1),
            format: .RGBA8,
            colorSpace: CGColorSpaceCreateDeviceRGB()
        )

        let r = Int(bitmap[0])
        let g = Int(bitmap[1])
        let b = Int(bitmap[2])
        return (0xFF << 24) | (r << 16) | (g << 8) | b
    }
}
