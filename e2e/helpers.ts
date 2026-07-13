import { expect, type BrowserContext, type Page } from "@playwright/test";

export async function openSettings(page: Page) {
  const pageErrors: string[] = [];
  page.on("pageerror", error => pageErrors.push(error.message));
  await page.addInitScript(() => {
    localStorage.clear();
    sessionStorage.clear();
    localStorage.setItem("audio_unlocked", "true");
    localStorage.setItem("keep_screen_on", "false");
    sessionStorage.setItem("pwa_prompted_session", "true");
  });
  await page.goto("/settings");
  return pageErrors;
}

export async function openNewTask(page: Page) {
  await page.getByRole("button", { name: "自定义任务", exact: true }).click();
  await page.getByRole("button", { name: "新建任务", exact: true }).click();
  await expect(page.getByRole("button", { name: "暂不创建", exact: true })).toBeVisible();
}

export async function dragTouchVertically(
  context: BrowserContext,
  page: Page,
  x: number,
  startY: number,
  endY: number,
) {
  const cdp = await context.newCDPSession(page);
  await cdp.send("Input.dispatchTouchEvent", {
    type: "touchStart",
    touchPoints: [{ x, y: startY, id: 1, radiusX: 1, radiusY: 1, force: 1 }],
  });
  for (let step = 1; step <= 12; step += 1) {
    await cdp.send("Input.dispatchTouchEvent", {
      type: "touchMove",
      touchPoints: [{
        x,
        y: startY + ((endY - startY) * step) / 12,
        id: 1,
        radiusX: 1,
        radiusY: 1,
        force: 1,
      }],
    });
  }
  await cdp.send("Input.dispatchTouchEvent", { type: "touchEnd", touchPoints: [] });
}
