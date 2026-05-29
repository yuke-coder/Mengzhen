'use client';

import { useAuth } from '@/lib/auth-context';
import { User } from 'lucide-react';

export function UserAvatar() {
  const { user } = useAuth();
  
  if (!user) return null;

  return (
    <div className="w-9 h-9 rounded-full bg-gradient-to-br from-pink-500 to-purple-500 flex items-center justify-center overflow-hidden shadow-md ring-2 ring-white/20">
      {user.avatar_url ? (
        <img 
          src={user.avatar_url} 
          alt={user.username} 
          className="w-full h-full object-cover"
        />
      ) : (
        <User className="w-4 h-4 text-white" />
      )}
    </div>
  );
}
