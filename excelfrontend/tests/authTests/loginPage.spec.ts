import { test, expect } from "@playwright/test";
import { dashboardURL, LoginPage, loginURL } from "../pages/loginPage.";

test('Login test valid email and password', async ({ page }) => {
    const loginPage = new LoginPage(page);

    await loginPage.goToLoginPage();

    await loginPage.loginUser('validemail@example.com', 'validpassword');

    await expect(page).toHaveURL(dashboardURL);

})

test('Login test with invalid email and password', async ({ page }) => {

    const loginPage = new LoginPage(page);

    await loginPage.goToLoginPage();

    await loginPage.loginUser('valiexample.c', 'valiord');

    await expect(page).toHaveURL(loginURL);

    const errorToast = page.locator('.error-toast');

    await expect(errorToast).toBeVisible();

    await expect(errorToast).toHaveText('Invalid email or password');
})

test('Login test with correct email and incorrect password', async ({ page }) => {

    const loginPage = new LoginPage(page);

    await loginPage.goToLoginPage();

    await loginPage.loginUser('valiexample.c', 'wrongpass');

    await expect(page).toHaveURL(loginURL);

    const errorToast = page.locator('.error-toast');

    await expect(errorToast).toBeVisible();

    await expect(errorToast).toHaveText('Invalid email or password');
})

test('Login test with incorrect email and correct password', async ({ page }) => {

    const loginPage = new LoginPage(page);

    await loginPage.goToLoginPage();

    await loginPage.loginUser('valiwrong@gmail.com', 'password');

    await expect(page).toHaveURL(loginURL);

    const errorToast = page.locator('.error-toast');

    await expect(errorToast).toBeVisible();

    await expect(errorToast).toHaveText('Invalid email or password');
})

test('Login test with empty email field', async ({ page }) => {

    const loginPage = new LoginPage(page);

    await loginPage.goToLoginPage();

    await loginPage.emailInput.click();

    await loginPage.emailInput.blur();

    const requiredEmailError = page.getByText('Email is required');

    await expect(requiredEmailError).toBeVisible();
})

test('Login test with empty password field', async ({ page }) => {

    const loginPage = new LoginPage(page);

    await loginPage.goToLoginPage();

    await loginPage.passwordInput.click();

    await loginPage.passwordInput.blur();

    const requiredPasswordError = page.getByText('Password is required');

    await expect(requiredPasswordError).toBeVisible();
})

test('Login test with empty email and password fields', async ({ page }) => {

    const loginPage = new LoginPage(page);

    await loginPage.goToLoginPage();

    await loginPage.emailInput.click();

    await loginPage.emailInput.blur();

    await loginPage.passwordInput.click();

    await loginPage.passwordInput.blur();

    const requiredEmailError = page.getByText('Email is required');

    await expect(requiredEmailError).toBeVisible();

    const requiredPasswordError = page.getByText('Password is required');

    await expect(requiredPasswordError).toBeVisible();
})

