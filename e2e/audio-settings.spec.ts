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
  await page.getByRole("button", { name: "暂不创建", exact: true }).click();
  await expect(page.getByRole("button", { name: "暂不创建", exact: true })).toBeHidden();
  await page.getByRole("button", { name: "默认设置", exact: true }).click();
  await expect(page.getByLabel("音量控制")).toHaveValue("37");
  await expect(audioHandles(page)).toHaveCount(25);
  expect(pageErrors).toEqual([]);
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
