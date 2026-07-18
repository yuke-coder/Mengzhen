import { expect, test, type BrowserContext, type Page } from "@playwright/test";
import { dragTouchVertically, openSettings } from "./helpers";

const AUDIO_HANDLE_NAME = /^调整「(.+)」的播放顺序$/;

function silentWav(name: string) {
  const sampleRate = 8_000;
  const sampleCount = 80;
  const buffer = Buffer.alloc(44 + sampleCount, 128);

  buffer.write("RIFF", 0);
  buffer.writeUInt32LE(36 + sampleCount, 4);
  buffer.write("WAVE", 8);
  buffer.write("fmt ", 12);
  buffer.writeUInt32LE(16, 16);
  buffer.writeUInt16LE(1, 20);
  buffer.writeUInt16LE(1, 22);
  buffer.writeUInt32LE(sampleRate, 24);
  buffer.writeUInt32LE(sampleRate, 28);
  buffer.writeUInt16LE(1, 32);
  buffer.writeUInt16LE(8, 34);
  buffer.write("data", 36);
  buffer.writeUInt32LE(sampleCount, 40);

  return { name, mimeType: "audio/wav", buffer };
}

function audioFiles(count: number) {
  return Array.from({ length: count }, (_, index) => (
    silentWav(`clip-${String(index + 1).padStart(2, "0")}.wav`)
  ));
}

function audioHandles(page: Page) {
  return page.getByRole("button", { name: AUDIO_HANDLE_NAME });
}

async function audioOrder(page: Page) {
  const labels = await audioHandles(page).evaluateAll(buttons => (
    buttons.map(button => button.getAttribute("aria-label") ?? "")
  ));
  return labels.map(label => label.match(AUDIO_HANDLE_NAME)?.[1] ?? label);
}

async function openAudioSettings(page: Page) {
  await page.addInitScript(() => {
    const testWindow = window as typeof window & { __previewPlayCalls: number };
    testWindow.__previewPlayCalls = 0;
    HTMLMediaElement.prototype.play = function play() {
      if (this.dataset.previewAudio) testWindow.__previewPlayCalls += 1;
      return Promise.resolve();
    };
  });
  const pageErrors = await openSettings(page);
  await expect(page.getByLabel("选择音频文件")).toBeAttached();
  return pageErrors;
}

async function uploadAudios(page: Page, count: number) {
  await page.getByLabel("选择音频文件").setInputFiles(audioFiles(count));
  await expect(audioHandles(page)).toHaveCount(count, { timeout: 30_000 });
}

async function setRangeValue(page: Page, value: number) {
  const slider = page.getByLabel("音量控制");
  await slider.fill(String(value));
  await expect(slider).toHaveValue(String(value));
  await expect.poll(() => page.evaluate(() => {
    const raw = localStorage.getItem("dream_default_play_config");
    if (!raw) return null;
    return (JSON.parse(raw) as { playback?: { volume?: number } }).playback?.volume ?? null;
  })).toBe(value);
}

async function previewPlayCalls(page: Page) {
  return page.evaluate(() => (
    (window as typeof window & { __previewPlayCalls: number }).__previewPlayCalls
  ));
}

async function dragWithMouse(page: Page, sourceName: string, targetName: string) {
  const source = page.getByRole("button", { name: `调整「${sourceName}」的播放顺序` });
  const target = page.getByRole("button", { name: `调整「${targetName}」的播放顺序` });
  await target.scrollIntoViewIfNeeded();
  const sourceBox = await source.boundingBox();
  const targetBox = await target.boundingBox();
  expect(sourceBox).not.toBeNull();
  expect(targetBox).not.toBeNull();
  if (!sourceBox || !targetBox) return;

  await page.mouse.move(sourceBox.x + sourceBox.width / 2, sourceBox.y + sourceBox.height / 2);
  await page.mouse.down();
  await page.mouse.move(targetBox.x + targetBox.width / 2, targetBox.y + targetBox.height / 2 + 8, { steps: 12 });
  await page.mouse.up();
}

async function dragWithTouch(context: BrowserContext, page: Page, sourceName: string, targetName: string) {
  const source = page.getByRole("button", { name: `调整「${sourceName}」的播放顺序` });
  const target = page.getByRole("button", { name: `调整「${targetName}」的播放顺序` });
  await target.scrollIntoViewIfNeeded();
  const sourceBox = await source.boundingBox();
  const targetBox = await target.boundingBox();
  expect(sourceBox).not.toBeNull();
  expect(targetBox).not.toBeNull();
  if (!sourceBox || !targetBox) return;

  const x = sourceBox.x + sourceBox.width / 2;
  const startY = sourceBox.y + sourceBox.height / 2;
  const endY = targetBox.y + targetBox.height / 2 + 8;
  await dragTouchVertically(context, page, x, startY, endY);
}

test("超过 20 个音频仍全部添加，默认设置与新建任务继续共享", async ({ page }) => {
  const pageErrors = await openAudioSettings(page);
  await uploadAudios(page, 25);
  await expect(page.getByText("已添加 25 个音频", { exact: true })).toBeVisible();
  expect(await audioOrder(page)).toEqual(audioFiles(25).map(file => file.name));

  await setRangeValue(page, 23);
  await page.getByRole("button", { name: "自定义任务", exact: true }).click();
  await expect(page.getByLabel("选择音频文件")).toHaveCount(0);
  await expect(page.getByLabel("音量控制")).toHaveCount(0);

  await page.getByRole("button", { name: "新建任务", exact: true }).click();
  await expect(page.getByRole("button", { name: "暂不创建", exact: true })).toBeVisible();
  await expect(page.getByLabel("选择音频文件")).toHaveCount(1);
  await expect(audioHandles(page)).toHaveCount(25);
  await expect(page.getByLabel("音量控制")).toHaveValue("23");
  expect(await audioOrder(page)).toEqual(audioFiles(25).map(file => file.name));

  await setRangeValue(page, 37);
  await page.keyboard.press("Escape");
  await expect(page.getByRole("button", { name: "暂不创建", exact: true })).toBeHidden();
  await page.getByRole("button", { name: "默认设置", exact: true }).click();
  await expect(page.getByLabel("音量控制")).toHaveValue("37");
  await expect(audioHandles(page)).toHaveCount(25);
  expect(pageErrors).toEqual([]);
});

test("选择音频只准备任务资源，手动点击后才存入音频库", async ({ page }) => {
  const userId = "00000000-0000-0000-0000-000000000001";
  let uploadTickets = 0;
  let directStorageUploads = 0;
  let completionRequests = 0;
  let librarySaves = 0;
  let savedFileKey: string | undefined;
  let releaseCompletion!: () => void;
  const completionResponse = new Promise<void>(resolve => {
    releaseCompletion = resolve;
  });

  await page.route("**/api/auth/me", route => route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify({
      success: true,
      user: { id: userId, username: "e2e-user", createdAt: new Date(0).toISOString() },
    }),
  }));
  await page.route("**/api/audio/upload-ticket", async route => {
    uploadTickets += 1;
    const requestUrl = new URL(route.request().url());
    const fileKey = `audios/${userId}/task-resource-${uploadTickets}.wav`;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        success: true,
        fileKey,
        uploadToken: "test-upload-token",
        tusEndpoint: `${requestUrl.origin}/api/test-tus`,
        bucket: "audios",
      }),
    });
  });
  await page.route("**/api/test-tus**", async route => {
    directStorageUploads += 1;
    const request = route.request();
    const uploadLength = request.headers()["upload-length"] || "0";
    await route.fulfill({
      status: 201,
      headers: {
        "Tus-Resumable": "1.0.0",
        "Upload-Offset": uploadLength,
        Location: `${new URL(request.url()).origin}/api/test-tus/session-1`,
      },
    });
  });
  await page.route("**/api/audio/upload-complete", async route => {
    completionRequests += 1;
    const { fileKey } = route.request().postDataJSON() as { fileKey?: string };
    await completionResponse;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        success: true,
        audio_url: `https://example.test/${fileKey}`,
        file_key: fileKey,
      }),
    });
  });
  await page.route("**/api/audio/save-to-library", async route => {
    librarySaves += 1;
    savedFileKey = (route.request().postDataJSON() as { fileKey?: string }).fileKey;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ success: true }),
    });
  });
  await page.route("**/api/audio/my-list", route => route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify({
      success: true,
      audios: savedFileKey ? [{
        id: "library-audio-1",
        title: "clip-01.wav",
        file_url: `https://example.test/${savedFileKey}`,
        file_key: savedFileKey,
        file_name: "clip-01.wav",
        file_size: silentWav("clip-01.wav").buffer.length,
        duration: 0,
        mime_type: "audio/wav",
        created_at: new Date(0).toISOString(),
      }] : [],
    }),
  }));

  const pageErrors = await openAudioSettings(page);
  await expect(page.locator("[data-user-menu-dropdown]")).toBeAttached();
  await uploadAudios(page, 1);

  await expect.poll(() => completionRequests).toBe(1);
  expect(uploadTickets).toBe(1);
  expect(directStorageUploads).toBeGreaterThan(0);
  expect(librarySaves).toBe(0);
  await expect(page.getByText("正在确认任务资源", { exact: true })).toBeVisible();
  await expect(page.getByText("已存音频库", { exact: true })).toHaveCount(0);

  releaseCompletion();
  await expect(page.getByText("正在确认任务资源", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "全部存入音频库", exact: true }).click();
  await expect.poll(() => librarySaves).toBe(1);
  expect(savedFileKey).toBe(`audios/${userId}/task-resource-1.wav`);
  await expect(page.getByText("已存音频库", { exact: true })).toBeVisible();

  await page.goto("/history");
  await expect(page.getByText("clip-01.wav", { exact: true })).toBeVisible();
  await expect(page.getByText("共 1 个已保存音频", { exact: true })).toBeVisible();
  expect(pageErrors).toEqual([]);
});

test("音频直传最多同时处理三份任务资源", async ({ page }) => {
  const userId = "00000000-0000-0000-0000-000000000002";
  let ticketRequests = 0;
  let directStarts = 0;
  let completionRequests = 0;
  const releaseDirectUploads: Array<() => void> = [];

  await page.route("**/api/auth/me", route => route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify({
      success: true,
      user: { id: userId, username: "queue-user", createdAt: new Date(0).toISOString() },
    }),
  }));
  await page.route("**/api/audio/upload-ticket", async route => {
    ticketRequests += 1;
    const requestUrl = new URL(route.request().url());
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        success: true,
        fileKey: `audios/${userId}/queued-${ticketRequests}.wav`,
        uploadToken: "queue-token",
        tusEndpoint: `${requestUrl.origin}/api/queued-test-tus`,
        bucket: "audios",
      }),
    });
  });
  await page.route("**/api/queued-test-tus**", async route => {
    directStarts += 1;
    await new Promise<void>(resolve => releaseDirectUploads.push(resolve));
    const request = route.request();
    await route.fulfill({
      status: 201,
      headers: {
        "Tus-Resumable": "1.0.0",
        "Upload-Offset": request.headers()["upload-length"] || "0",
        Location: `${new URL(request.url()).origin}/api/queued-test-tus/session-${directStarts}`,
      },
    });
  });
  await page.route("**/api/audio/upload-complete", async route => {
    completionRequests += 1;
    const { fileKey } = route.request().postDataJSON() as { fileKey?: string };
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ success: true, audio_url: `https://example.test/${fileKey}`, file_key: fileKey }),
    });
  });

  const pageErrors = await openAudioSettings(page);
  await uploadAudios(page, 4);
  await expect.poll(() => directStarts).toBe(3);
  expect(ticketRequests).toBe(3);

  releaseDirectUploads[0]();
  await expect.poll(() => directStarts).toBe(4);
  expect(ticketRequests).toBe(4);

  for (const release of releaseDirectUploads) release();
  await expect.poll(() => completionRequests).toBe(4);
  await expect(page.getByText("正在传输任务资源", { exact: true })).toHaveCount(0);
  expect(pageErrors).toEqual([]);
});

test("音频库请求失败时显示中文错误，不伪装成空库", async ({ page }) => {
  await page.route("**/api/auth/me", route => route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify({
      success: true,
      user: {
        id: "00000000-0000-0000-0000-000000000001",
        username: "e2e-user",
        createdAt: new Date(0).toISOString(),
      },
    }),
  }));
  await page.route("**/api/audio/my-list", route => route.fulfill({
    status: 500,
    contentType: "application/json",
    body: JSON.stringify({ success: false, error: "upstream query failed" }),
  }));

  await page.goto("/history");
  await expect(page.getByText("音频库加载失败，请重试", { exact: true })).toBeVisible();
  await expect(page.getByText("暂无音频记录", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "重新加载", exact: true })).toBeVisible();
});

test("音频库记录为 0 时显示可操作的正常空状态", async ({ page }) => {
  await page.route("**/api/auth/me", route => route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify({
      success: true,
      user: {
        id: "00000000-0000-0000-0000-000000000001",
        username: "e2e-user",
        createdAt: new Date(0).toISOString(),
      },
    }),
  }));
  await page.route("**/api/audio/my-list", route => route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify({ success: true, audios: [] }),
  }));

  await page.goto("/history");
  await expect(page.getByText("音频库还是空的", { exact: true })).toBeVisible();
  await expect(page.getByText(/选择音频只会为任务准备播放资源/)).toBeVisible();
  await expect(page.getByRole("link", { name: "返回设置", exact: true })).toHaveAttribute("href", "/settings");
  await expect(page.getByText("音频库加载失败，请重试", { exact: true })).toHaveCount(0);
});

test("鼠标和键盘排序不会误触试听", async ({ page }) => {
  const pageErrors = await openAudioSettings(page);
  await uploadAudios(page, 3);
  const playCallsBeforeDrag = await previewPlayCalls(page);
  const orderBeforeDrag = await audioOrder(page);

  await dragWithMouse(page, "clip-01.wav", "clip-03.wav");
  await expect.poll(async () => (await audioOrder(page)).indexOf("clip-01.wav")).toBeGreaterThan(0);
  const orderAfterDrag = await audioOrder(page);
  expect([...orderAfterDrag].sort()).toEqual([...orderBeforeDrag].sort());
  await expect(page.getByRole("button", { name: /^暂停「/ })).toHaveCount(0);
  await expect.poll(() => previewPlayCalls(page)).toBe(playCallsBeforeDrag);

  const indexBeforeKeyboard = orderAfterDrag.indexOf("clip-01.wav");
  const movedHandle = page.getByRole("button", { name: "调整「clip-01.wav」的播放顺序" });
  await movedHandle.focus();
  await movedHandle.press("ArrowUp");
  await expect.poll(async () => (await audioOrder(page)).indexOf("clip-01.wav")).toBe(indexBeforeKeyboard - 1);
  expect(pageErrors).toEqual([]);
});

test.describe("触摸排序", () => {
  test.use({ viewport: { width: 390, height: 844 }, hasTouch: true, isMobile: true });

  test("真实触摸拖动使用同一条排序路径且不会误触试听", async ({ context, page }) => {
    const pageErrors = await openAudioSettings(page);
    await uploadAudios(page, 3);
    const playCallsBeforeDrag = await previewPlayCalls(page);
    const orderBeforeDrag = await audioOrder(page);

    await dragWithTouch(context, page, "clip-01.wav", "clip-03.wav");
    await expect.poll(async () => (await audioOrder(page)).indexOf("clip-01.wav")).toBeGreaterThan(0);
    const orderAfterDrag = await audioOrder(page);
    expect([...orderAfterDrag].sort()).toEqual([...orderBeforeDrag].sort());
    await expect(page.getByRole("button", { name: /^暂停「/ })).toHaveCount(0);
    await expect.poll(() => previewPlayCalls(page)).toBe(playCallsBeforeDrag);
    expect(pageErrors).toEqual([]);
  });
});
