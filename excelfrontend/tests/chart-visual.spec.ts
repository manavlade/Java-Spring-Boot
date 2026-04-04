import { expect, test, } from "@playwright/test";
import * as fs from "node:fs";
import path from "node:path";
import { environment } from "../src/environments/environment";

const timeStamp = new Date();

const baseURL = `${environment.BASE_URL}`;

test('chart visual test', async ({ page }) => {

    await page.goto(baseURL);

    await page.waitForSelector('#main', { state: 'visible' });

    await page.waitForTimeout(3000);

    const baseline = await page.locator('#main').screenshot({ path: 'screenshots/baseline.png' });

    await page.reload();

    await page.waitForSelector('#main', { state: 'visible' });

    await page.waitForTimeout(3000);

    const afterReload = await page.locator('#main').screenshot({ path: 'screenshots/after-reload.png' });

    expect(baseline).toMatchSnapshot('baseline.png');
    expect(afterReload).toMatchSnapshot('after-reload.png');
})


test('Chart Visual test with allowed pixel difference', async ({ page }) => {

    const baselinePath = path.join('screenshots', 'baseline.png');

    await page.goto(baseURL);

    await page.waitForSelector('#main', { state: 'visible' });

    await page.waitForTimeout(3000);

    if (!fs.existsSync(baselinePath)) {

        await page.locator('#main').screenshot({ path: baselinePath });

        console.log('Baseline created! Run the test again to start comparing.');
        return;
    }

    await page.locator('#main').screenshot({ path: 'screenshots/latest.png' });


    await expect(page.locator('#main')).toHaveScreenshot('baseline.png', {
        maxDiffPixels: 100,
    })
})