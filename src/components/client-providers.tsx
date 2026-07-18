'use client';

import { useEffect } from 'react';
import dynamic from 'next/dynamic';

const DynamicBackground = dynamic(() => import('@/components/dynamic-background'), {
    ssr: false,
    loading: () => <div className="fixed inset-0 z-0" />,
});

export default function ClientProviders({ children }: { children: React.ReactNode }) {
    useEffect(() => {
        const handler = (e: Event) => {
            const t = e.target as HTMLElement;
            if (!t.closest('input, textarea, [contenteditable]')) e.preventDefault();
        };
        document.addEventListener('contextmenu', handler);
        return () => document.removeEventListener('contextmenu', handler);
    }, []);

    return (
        <>
            <div className="fixed inset-0 overflow-hidden z-0 pointer-events-none">
                <DynamicBackground />
            </div>
            {children}
        </>
    );
}
