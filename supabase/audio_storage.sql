-- Keep the Storage bucket aligned with the audio extensions accepted by Web and Android.
update storage.buckets
set allowed_mime_types = array[
  'audio/mpeg',
  'audio/mp3',
  'audio/wav',
  'audio/x-wav',
  'audio/vnd.wave',
  'audio/ogg',
  'application/ogg',
  'audio/mp4',
  'audio/x-m4a',
  'audio/flac',
  'audio/x-flac',
  'audio/aac',
  'audio/mp4a-latm'
]::text[]
where id = 'audios';
