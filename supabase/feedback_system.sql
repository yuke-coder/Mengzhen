BEGIN;

CREATE TABLE IF NOT EXISTS public.feedbacks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  type VARCHAR(20) NOT NULL CHECK (type IN ('bug', 'suggestion')),
  category VARCHAR(80),
  content TEXT NOT NULL,
  contact TEXT,
  images TEXT[],
  status SMALLINT NOT NULL DEFAULT 1 CHECK (status BETWEEN 1 AND 3),
  op_group TEXT,
  op_name TEXT,
  processed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public.feedbacks
  ADD COLUMN IF NOT EXISTS category VARCHAR(80),
  ADD COLUMN IF NOT EXISTS status SMALLINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS op_group TEXT,
  ADD COLUMN IF NOT EXISTS op_name TEXT,
  ADD COLUMN IF NOT EXISTS processed_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE TABLE IF NOT EXISTS public.feedback_replies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  feedback_id UUID NOT NULL REFERENCES public.feedbacks(id) ON DELETE CASCADE,
  user_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
  sender_role VARCHAR(20) NOT NULL DEFAULT 'user'
    CHECK (sender_role IN ('user', 'support')),
  sender VARCHAR(80) NOT NULL,
  content TEXT NOT NULL,
  images TEXT[],
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_feedbacks_user_created
  ON public.feedbacks(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_feedback_replies_feedback_created
  ON public.feedback_replies(feedback_id, created_at ASC);

ALTER TABLE public.feedbacks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.feedback_replies ENABLE ROW LEVEL SECURITY;

COMMIT;

NOTIFY pgrst, 'reload schema';
