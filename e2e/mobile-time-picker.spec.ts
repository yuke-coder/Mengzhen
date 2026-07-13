import { expect, test, type BrowserContext, type Locator, type Page } from "@playwright/test";

async function swipeVertically(context: BrowserContext, target: Locator, distance: number) {
  await target.evaluate(element => element.scrollIntoView({ block: "center" }));
  const box = await target.boundingBox();
  expect(box).not.toBeNull();
  if (!box) return;

  const cdp = await context.newCDPSession(target.page());
  const x = box.x + box.width / 2;
  const startY = box.y + box.height / 2;
  await cdp.send("Input.dispatchTouchEvent", {
    type: "touchStart",
    touchPoints: [{ x, y: startY, id: 1, radiusX: 1, radiusY: 1, force: 1 }],
  });
  for (let step = 1; step <= 12; step += 1) {
    await cdp.send("Input.dispatchTouchEvent", {
      type: "touchMove",
      touchPoints: [{
        x,
        y: startY + (distance * step) / 12,
        id: 1,
        radiusX: 1,
        radiusY: 1,
        force: 1,
      }],
    });
  }
  await cdp.send("Input.dispatchTouchEvent", { type: "touchEnd", touchPoints: [] });
}

async function openNewTask(page: Page) {
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
  await expect(page.getByRole("group", { name: "开始时间滚轮选择器" })).toBeVisible();
  await page.getByRole("button", { name: "自定义任务", exact: true }).click();
  await page.getByRole("button", { name: "新建任务", exact: true }).click();
  await expect(page.getByRole("button", { name: "暂不创建", exact: true })).toBeVisible();
  return pageErrors;
}

for (const width of [390, 711]) {
  test.describe(`${width}px 移动端新建任务`, () => {
    test.use({ viewport: { width, height: 844 }, hasTouch: true, isMobile: true });

    test("时间滚轮默认收起，且摘要区域仍能滚动整体 Sheet", async ({ context, page }) => {
      const pageErrors = await openNewTask(page);
      const picker = page.getByRole("group", { name: "开始时间滚轮选择器", includeHidden: true });
      const expandButton = page.getByRole("button", { name: /^展开开始时间选择器/ });

      await expect(expandButton).toBeVisible();
      await expect(expandButton).toHaveAttribute("aria-expanded", "false");
      await expect(expandButton).toHaveAttribute("aria-label", /^展开开始时间选择器，当前值 \d{1,2}月\d{1,2}日 \d{2}:\d{2}$/);
      await expect(picker).toBeAttached();
      await expect(picker).toBeHidden();
      expect(await expandButton.getAttribute("aria-controls")).toBe(await picker.getAttribute("id"));

      const collapsedBox = await expandButton.boundingBox();
      expect(collapsedBox).not.toBeNull();
      if (collapsedBox) expect(collapsedBox.height).toBeGreaterThanOrEqual(44);

      const initialLabel = await expandButton.getAttribute("aria-label");

      await expandButton.tap();
      const collapseButton = page.getByRole("button", { name: /^收起开始时间选择器/ });
      await expect(collapseButton).toHaveAttribute("aria-expanded", "true");
      await expect(picker).toBeVisible();

      await collapseButton.tap();
      const collapsedButton = page.getByRole("button", { name: /^展开开始时间选择器/ });
      await expect(collapsedButton).toHaveAttribute("aria-expanded", "false");
      await expect(collapsedButton).toHaveAttribute("aria-label", initialLabel ?? "");
      await expect(picker).toBeHidden();

      await collapsedButton.focus();
      await collapsedButton.press("Enter");
      await expect(page.getByRole("button", { name: /^收起开始时间选择器/ })).toHaveAttribute("aria-expanded", "true");
      await page.getByRole("button", { name: /^收起开始时间选择器/ }).press("Space");
      await expect(collapsedButton).toHaveAttribute("aria-expanded", "false");

      const sheetScroller = page.locator("[data-vaul-no-drag]");
      const metrics = await sheetScroller.evaluate(element => ({
        scrollTop: element.scrollTop,
        maxScroll: element.scrollHeight - element.clientHeight,
      }));
      expect(metrics.maxScroll).toBeGreaterThan(40);
      const canScrollDown = metrics.maxScroll - metrics.scrollTop > 80;
      await swipeVertically(context, collapsedButton, canScrollDown ? -100 : 100);
      await expect.poll(async () => {
        const nextScrollTop = await sheetScroller.evaluate(element => element.scrollTop);
        return Math.abs(nextScrollTop - metrics.scrollTop);
      }).toBeGreaterThan(20);
      await expect(collapsedButton).toHaveAttribute("aria-expanded", "false");

      await page.keyboard.press("Escape");
      await expect(page.getByRole("button", { name: "暂不创建", exact: true })).toBeHidden();
      await page.getByRole("button", { name: "新建任务", exact: true }).click();
      await expect(page.getByRole("button", { name: /^展开开始时间选择器/ })).toHaveAttribute("aria-expanded", "false");
      expect(pageErrors).toEqual([]);
    });

    test("半屏 Sheet 滚到底后底部操作区仍完整可见", async ({ context, page }) => {
      const pageErrors = await openNewTask(page);
      const sheet = page.locator("[data-vaul-drawer]");
      const sheetScroller = page.locator("[data-vaul-no-drag]");
      const cancelButton = page.getByRole("button", { name: "暂不创建", exact: true });

      await expect.poll(async () => sheet.evaluate(element => {
        const snapOffset = Number.parseFloat(
          getComputedStyle(element).getPropertyValue("--snap-point-height")
        );
        return Math.abs(element.getBoundingClientRect().top - snapOffset);
      })).toBeLessThan(1);

      await sheetScroller.evaluate(element => {
        element.scrollTop = element.scrollHeight;
      });

      await expect.poll(async () => sheetScroller.evaluate(element => element.scrollTop))
        .toBeGreaterThan(200);

      const geometry = await page.evaluate(() => {
        const drawer = document.querySelector<HTMLElement>("[data-vaul-drawer]");
        const scroller = document.querySelector<HTMLElement>("[data-vaul-no-drag]");
        const cancel = Array.from(document.querySelectorAll("button"))
          .find(button => button.textContent?.trim() === "暂不创建");
        if (!drawer || !scroller || !cancel) throw new Error("Task Sheet geometry is incomplete");

        const drawerRect = drawer.getBoundingClientRect();
        const scrollerRect = scroller.getBoundingClientRect();
        const cancelRect = cancel.getBoundingClientRect();
        return {
          viewportBottom: window.innerHeight,
          drawerTop: drawerRect.top,
          scrollerBottom: scrollerRect.bottom,
          cancelTop: cancelRect.top,
          cancelBottom: cancelRect.bottom,
          scrollTop: scroller.scrollTop,
          maxScroll: scroller.scrollHeight - scroller.clientHeight,
        };
      });

      expect(geometry.drawerTop).toBeGreaterThan(0);
      expect(geometry.scrollTop).toBe(geometry.maxScroll);
      expect(geometry.scrollerBottom).toBeLessThanOrEqual(geometry.viewportBottom + 1);
      expect(geometry.cancelTop).toBeGreaterThan(geometry.drawerTop);
      expect(geometry.cancelBottom).toBeLessThanOrEqual(geometry.viewportBottom);
      await expect(sheet).toHaveAttribute("data-fullscreen", "false");
      await expect(cancelButton).toBeVisible();

      const drawerHandle = sheet.locator(":scope > div").first();
      await swipeVertically(context, drawerHandle, -400);
      await expect(sheet).toHaveAttribute("data-fullscreen", "true");
      await expect.poll(async () => sheet.evaluate(element => (
        Math.abs(element.getBoundingClientRect().top)
      ))).toBeLessThan(1);

      const fullGeometry = await sheetScroller.evaluate(element => {
        const drawer = element.closest<HTMLElement>("[data-vaul-drawer]");
        if (!drawer) throw new Error("Task Sheet is missing");
        return {
          paddingBottom: Number.parseFloat(getComputedStyle(drawer).paddingBottom),
          scrollerBottom: element.getBoundingClientRect().bottom,
          viewportBottom: window.innerHeight,
        };
      });
      expect(fullGeometry.paddingBottom).toBe(0);
      expect(fullGeometry.scrollerBottom).toBeLessThanOrEqual(fullGeometry.viewportBottom + 1);
      expect(pageErrors).toEqual([]);
    });
  });
}

test("桌面端新建任务仍直接显示时间滚轮", async ({ page }) => {
  const pageErrors = await openNewTask(page);
  await expect(page.getByRole("button", { name: /开始时间选择器/ })).toHaveCount(0);
  await expect(page.getByRole("group", { name: "开始时间滚轮选择器" })).toBeVisible();
  expect(pageErrors).toEqual([]);
});
