export interface FeedbackSummary {
  id: string;
  type: 'bug' | 'suggestion';
  category: string | null;
  content: string;
  contact: string | null;
  images: string[] | null;
  status: 1 | 2 | 3;
  op_group: string | null;
  op_name: string | null;
  processed_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface FeedbackReply {
  id: string;
  feedback_id: string;
  sender_role: 'user' | 'support';
  sender: string;
  content: string;
  images: string[] | null;
  created_at: string;
}

export const feedbackStatusLabel = (status: number) => {
  if (status === 2) return '受理中';
  if (status === 3) return '受理完毕';
  return '尚未受理';
};

export const feedbackTypeLabel = (type: string) =>
  type === 'bug' ? 'Bug 缺陷' : '产品建议';

export const formatFeedbackTime = (value: string | null) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
};
