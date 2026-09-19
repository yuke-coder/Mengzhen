-- Preserve source-provided timed lyrics alongside uploaded audio.
-- The Android importer sends normalized LRC only; no model/ASR output is stored.
ALTER TABLE public.audios
  ADD COLUMN IF NOT EXISTS lyric_text TEXT;
