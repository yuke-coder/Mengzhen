// 缓存版本号 - 每次部署时由构建脚本自动更新
// 格式: mengzhen-v{timestamp}
const CACHE_VERSION = 'v4';
const CACHE_NAME = 'mengzhen-' + CACHE_VERSION;
const STATIC_CACHE = 'mengzhen-static-' + CACHE_VERSION;
const AUDIO_CACHE = 'mengzhen-audio-' + CACHE_VERSION;
const API_CACHE = 'mengzhen-api-' + CACHE_VERSION;

const CRITICAL_ASSETS = [
  '/',
  '/manifest.json',
  '/favicon.png',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE).then(async (cache) => {
      for (const url of CRITICAL_ASSETS) {
        try { await cache.add(url); } catch (e) {}
      }
      self.skipWaiting();
    })
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames
          .filter((name) =>
            name !== CACHE_NAME &&
            name !== STATIC_CACHE &&
            name !== AUDIO_CACHE &&
            name !== API_CACHE
          )
          .map((name) => caches.delete(name))
      );
    }).then(() => {
      return self.clients.claim();
    })
  );
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  if (event.request.url.match(/\.(mp3|wav|flac|ogg|m4a|aac)$/i)) {
    event.respondWith(
      caches.open(AUDIO_CACHE).then((cache) => {
        return cache.match(event.request).then((cached) => {
          const fetchPromise = fetch(event.request).then((response) => {
            if (response.ok) {
              cache.put(event.request, response.clone());
            }
            return response;
          }).catch(() => null);

          return cached || fetchPromise;
        });
      })
    );
    return;
  }

  if (event.request.method !== 'GET') return;

  if (url.origin !== location.origin) {
    event.respondWith(
      caches.match(event.request).then((cached) => {
        if (cached) return cached;

        return fetch(event.request).then((response) => {
          if (response.ok) {
            const clone = response.clone();
            caches.open(STATIC_CACHE).then((cache) => {
              cache.put(event.request, clone);
            });
          }
          return response;
        }).catch(() => {
          return new Response('', { status: 200 });
        });
      })
    );
    return;
  }

  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(event.request)
        .then((response) => {
          if (response.ok) {
            const clone = response.clone();
            caches.open(API_CACHE).then((cache) => {
              cache.put(event.request, clone);
            });
          }
          return response;
        })
        .catch(() => {
          return caches.match(event.request).then((cached) => {
            if (cached) return cached;
            return new Response(JSON.stringify({ offline: true }), {
              status: 200,
              headers: { 'Content-Type': 'application/json' }
            });
          });
        })
    );
    return;
  }

  if (event.request.mode === 'navigate') {
    event.respondWith(
      fetch(event.request)
        .then((response) => {
          if (response.ok) {
            const clone = response.clone();
            caches.open(STATIC_CACHE).then((cache) => {
              cache.put(event.request, clone);
            });
          }
          return response;
        })
        .catch(() => {
          return caches.match(event.request).then((cached) => {
            if (cached) return cached;
            return caches.match('/').then((homePage) => {
              if (homePage) return homePage;
              return new Response('Offline - Please check your connection', {
                status: 503,
                headers: { 'Content-Type': 'text/html' }
              });
            });
          });
        })
    );
    return;
  }

  event.respondWith(
    caches.match(event.request).then((cached) => {
      if (cached) return cached;

      return fetch(event.request).then((response) => {
        if (response.ok && event.request.url.match(/\.(js|css|png|jpg|jpeg|gif|svg|woff2?|ttf|eot)$/i)) {
          const clone = response.clone();
          caches.open(STATIC_CACHE).then((cache) => {
            cache.put(event.request, clone);
          });
        }
        return response;
      }).catch(() => {
        return new Response('', { status: 200 });
      });
    })
  );
});

self.addEventListener('sync', (event) => {
  if (event.tag === 'audio-sync') {
    event.waitUntil(syncAudioData());
  }
});

async function syncAudioData() {
  console.log('Syncing audio data...');
}

self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }

  if (event.data && event.data.type === 'CACHE_URLS') {
    caches.open(STATIC_CACHE).then((cache) => {
      Promise.all(
        event.data.urls.map((url) =>
          cache.add(url).catch(() => null)
        )
      );
    });
  }
});
