import { expect, test, type Locator, type Page } from "@playwright/test";

async function openDurationSetter(page: Page) {
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
  await page.getByRole("button", { name: "自定义任务", exact: true }).click();
  await page.getByRole("button", { name: "新建任务", exact: true }).click();
  await expect(page.getByRole("button", { name: "暂不创建", exact: true })).toBeVisible();

  const duration = page.getByRole("group", { name: "播放时长" });
  await expect(duration).toBeVisible();
  return { duration, pageErrors };
}

function formattedDuration(minutes: number) {
  if (minutes < 60) return `${minutes}分钟`;
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return `${hours}小时${remainder ? `${remainder}分钟` : ""}`;
}

function right(box: { x: number; width: number }) {
  return box.x + box.width;
}

async function expectContained(locator: Locator, container: Locator) {
  const [box, containerBox] = await Promise.all([locator.boundingBox(), container.boundingBox()]);
  expect(box).not.toBeNull();
  expect(containerBox).not.toBeNull();
  if (!box || !containerBox) return;
  expect(box.x).toBeGreaterThanOrEqual(containerBox.x - 1);
  expect(right(box)).toBeLessThanOrEqual(right(containerBox) + 1);
}

test("分钟与小时共享同一个整数分钟值，单击加减只执行一步", async ({ page }) => {
  const { duration, pageErrors } = await openDurationSetter(page);
  const slider = duration.getByRole("slider", { name: "播放时长滑杆（分钟）" });
  const status = duration.getByRole("status");

  await expect(slider).toHaveValue("30");
  await expect(duration.getByRole("spinbutton", { name: "播放时长（分钟）" })).toHaveValue("30");
  await expect(status).toHaveText("30分钟");

  await duration.getByRole("button", { name: "切换为小时" }).click();
  await expect(slider).toHaveValue("30");
  await expect(duration.getByRole("spinbutton", { name: "播放时长（小时）" })).toHaveValue("0.5");

  await duration.getByRole("button", { name: "增加播放时长（每次1小时）" }).click();
  await expect(slider).toHaveValue("90");
  await expect(duration.getByRole("spinbutton", { name: "播放时长（小时）" })).toHaveValue("1.5");
  await expect(status).toHaveText("1小时30分钟");

  await duration.getByRole("button", { name: "减少播放时长（每次1小时）" }).click();
  await expect(slider).toHaveValue("30");
  await duration.getByRole("button", { name: "切换为分钟" }).click();

  await duration.getByRole("button", { name: "增加播放时长（每次1分钟）" }).click();
  await expect(slider).toHaveValue("31");
  await duration.getByRole("button", { name: "减少播放时长（每次1分钟）" }).click();
  await expect(slider).toHaveValue("30");
  expect(pageErrors).toEqual([]);
});

test("小时显示没有无限小数，切回分钟也不丢精度", async ({ page }) => {
  const { duration, pageErrors } = await openDurationSetter(page);
  const slider = duration.getByRole("slider", { name: "播放时长滑杆（分钟）" });
  const status = duration.getByRole("status");
  const cases = [
    [1, "0.02"],
    [30, "0.5"],
    [59, "0.98"],
    [60, "1"],
    [61, "1.02"],
    [90, "1.5"],
    [119, "1.98"],
    [299, "4.98"],
    [300, "5"],
  ] as const;

  for (const [minutes, hours] of cases) {
    const minuteInput = duration.getByRole("spinbutton", { name: "播放时长（分钟）" });
    await minuteInput.fill(String(minutes));
    await minuteInput.blur();
    await expect(slider).toHaveValue(String(minutes));
    await expect(status).toHaveText(formattedDuration(minutes));

    await duration.getByRole("button", { name: "切换为小时" }).click();
    const hourInput = duration.getByRole("spinbutton", { name: "播放时长（小时）" });
    await expect(hourInput).toHaveValue(hours);
    await expect(slider).toHaveValue(String(minutes));
    await expect(status).toHaveText(formattedDuration(minutes));
    expect(await hourInput.evaluate(input => (input as HTMLInputElement).validity.valid)).toBe(true);

    await duration.getByRole("button", { name: "切换为分钟" }).click();
    await expect(duration.getByRole("spinbutton", { name: "播放时长（分钟）" })).toHaveValue(String(minutes));
  }

  await duration.getByRole("button", { name: "切换为小时" }).click();
  const hourInput = duration.getByRole("spinbutton", { name: "播放时长（小时）" });
  await hourInput.fill("1.25");
  await expect(slider).toHaveValue("75");
  await expect(status).toHaveText("1小时15分钟");
  await duration.getByRole("button", { name: "增加播放时长（每次1小时）" }).click();
  await expect(slider).toHaveValue("135");
  await expect(hourInput).toHaveValue("2.25");
  await expect(status).toHaveText("2小时15分钟");
  await duration.getByRole("button", { name: "减少播放时长（每次1小时）" }).click();
  await expect(slider).toHaveValue("75");
  await duration.getByRole("button", { name: "切换为分钟" }).click();
  await expect(duration.getByRole("spinbutton", { name: "播放时长（分钟）" })).toHaveValue("75");
  expect(pageErrors).toEqual([]);
});

test("滑杆、输入框和加减按钮都守住 1 到 300 分钟边界", async ({ page }) => {
  const { duration, pageErrors } = await openDurationSetter(page);
  const slider = duration.getByRole("slider", { name: "播放时长滑杆（分钟）" });
  const minuteInput = duration.getByRole("spinbutton", { name: "播放时长（分钟）" });

  await slider.focus();
  await slider.press("Home");
  await expect(slider).toHaveValue("1");
  await expect(duration.getByRole("button", { name: "减少播放时长（每次1分钟）" })).toBeDisabled();
  await slider.press("ArrowLeft");
  await expect(slider).toHaveValue("1");
  await slider.press("ArrowRight");
  await expect(slider).toHaveValue("2");

  await slider.press("End");
  await expect(slider).toHaveValue("300");
  await expect(duration.getByRole("button", { name: "增加播放时长（每次1分钟）" })).toBeDisabled();
  await slider.press("ArrowRight");
  await expect(slider).toHaveValue("300");

  await minuteInput.fill("0");
  await minuteInput.blur();
  await expect(slider).toHaveValue("1");
  await expect(minuteInput).toHaveValue("1");
  await minuteInput.fill("301");
  await minuteInput.blur();
  await expect(slider).toHaveValue("300");

  await duration.getByRole("button", { name: "切换为小时" }).click();
  const hourInput = duration.getByRole("spinbutton", { name: "播放时长（小时）" });
  await hourInput.fill("0");
  await hourInput.blur();
  await expect(slider).toHaveValue("1");
  await expect(hourInput).toHaveValue("0.02");
  await hourInput.fill("5.01");
  await hourInput.blur();
  await expect(slider).toHaveValue("300");
  await expect(hourInput).toHaveValue("5");
  expect(pageErrors).toEqual([]);
});

for (const width of [320, 711]) {
  test.describe(`${width}px 窄屏布局`, () => {
    test.use({ viewport: { width, height: 800 }, hasTouch: true, isMobile: true });

    test("分钟和小时状态都不重叠、不裁断、不产生横向滚动", async ({ page }) => {
      const { duration, pageErrors } = await openDurationSetter(page);
      const minuteInput = duration.getByRole("spinbutton", { name: "播放时长（分钟）" });
      await minuteInput.fill("299");
      await minuteInput.blur();
      await duration.scrollIntoViewIfNeeded();

      for (const unit of ["分钟", "小时"] as const) {
        const input = duration.getByRole("spinbutton", { name: `播放时长（${unit}）` });
        const slider = duration.getByRole("slider", { name: "播放时长滑杆（分钟）" });
        const decrease = duration.getByRole("button", { name: new RegExp("^减少播放时长") });
        const increase = duration.getByRole("button", { name: new RegExp("^增加播放时长") });
        const unitButton = duration.getByRole("button", { name: unit === "分钟" ? "切换为小时" : "切换为分钟" });

        const dimensions = await duration.evaluate(element => ({
          clientWidth: element.clientWidth,
          scrollWidth: element.scrollWidth,
          bodyClientWidth: document.body.clientWidth,
          bodyScrollWidth: document.body.scrollWidth,
        }));
        expect(dimensions.scrollWidth).toBeLessThanOrEqual(dimensions.clientWidth + 1);
        expect(dimensions.bodyScrollWidth).toBeLessThanOrEqual(dimensions.bodyClientWidth + 1);

        for (const control of [slider, decrease, input, unitButton, increase]) {
          await expectContained(control, duration);
        }

        const [sliderBox, decreaseBox, inputBox, unitBox, increaseBox] = await Promise.all([
          slider.boundingBox(),
          decrease.boundingBox(),
          input.boundingBox(),
          unitButton.boundingBox(),
          increase.boundingBox(),
        ]);
        expect(sliderBox && decreaseBox && inputBox && unitBox && increaseBox).toBeTruthy();
        if (sliderBox && decreaseBox && inputBox && unitBox && increaseBox) {
          expect(right(sliderBox)).toBeLessThanOrEqual(decreaseBox.x + 1);
          expect(right(decreaseBox)).toBeLessThanOrEqual(inputBox.x + 1);
          expect(right(inputBox)).toBeLessThanOrEqual(unitBox.x + 1);
          expect(right(unitBox)).toBeLessThanOrEqual(increaseBox.x + 1);
        }

        if (unit === "分钟") {
          await unitButton.tap();
          await expect(duration.getByRole("spinbutton", { name: "播放时长（小时）" })).toHaveValue("4.98");
        }
      }
      expect(pageErrors).toEqual([]);
    });
  });
}
