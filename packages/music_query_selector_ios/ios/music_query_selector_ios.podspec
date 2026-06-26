Pod::Spec.new do |s|
  s.name             = 'music_query_selector_ios'
  s.version          = '1.1.1'
  s.summary          = 'music_query_selector flutter plugin for iOS.'
  s.description      = <<-DESC
  Flutter plugin used to query audios/songs info [title, artist, album, etc.] from the device media library.
                       DESC
  s.homepage         = 'https://github.com/CesarArellano/music_query_selector'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Cesar Arellano' => 'cesarmauricio.arellano@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'music_query_selector_ios/Sources/music_query_selector_ios/**/*.swift'
  s.dependency 'Flutter'
  s.platform = :ios, '12.0'

  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.9'
end
