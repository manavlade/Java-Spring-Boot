import { test, expect } from "@playwright/test";
import { SignUpPage } from "../pages/signUpPage";
import { dashboardURL } from "../pages/loginPage.";

test('Sign Up test valid email and password', async ({ page }) => {
    const signUpPage = new SignUpPage(page);

    await signUpPage.goToSignUpPage();

    await signUpPage.signUpUser('validemail@example.com', 'validpassword');

    await expect(page).toHaveURL(dashboardURL);

})

test('Sign Up test with empty email field', async ({ page }) => {

    const signUpPage = new SignUpPage(page);

    await signUpPage.goToSignUpPage();

    await signUpPage.emailInput.click();

    await signUpPage.emailInput.blur();

    const requiredEmailError = page.getByText('Email is required');

    await expect(requiredEmailError).toBeVisible();
})

test('Sign Up test with empty password field', async ({ page }) => {

    const signUpPage = new SignUpPage(page);

    await signUpPage.goToSignUpPage();

    await signUpPage.passwordInput.click();

    await signUpPage.passwordInput.blur();

    const requiredPasswordError = page.getByText('Password is required');

    await expect(requiredPasswordError).toBeVisible();
})

test('Sign Up test with empty email and password fields', async ({ page }) => {

    const signUpPage = new SignUpPage(page);

    await signUpPage.goToSignUpPage();

    await signUpPage.emailInput.click();

    await signUpPage.emailInput.blur();

    await signUpPage.passwordInput.click();

    await signUpPage.passwordInput.blur();

    const requiredEmailError = page.getByText('Email is required');

    await expect(requiredEmailError).toBeVisible();

    const requiredPasswordError = page.getByText('Password is required');

    await expect(requiredPasswordError).toBeVisible();
})

test("Sign Up test with password length less than 8 characters", async ({page}) => {

    const signUpPage = new SignUpPage(page);

    await signUpPage.goToSignUpPage();

    await signUpPage.passwordInput.fill('12345');

    await signUpPage.passwordInput.blur();

    const passwordLengthError = page.getByText('Password must be at least 8 characters long');

    await expect(passwordLengthError).toBeVisible();
})

test("Sign Up test with weak password", async ({ page }) => {

    const signUpPage = new SignUpPage(page);

    await signUpPage.goToSignUpPage();

    await signUpPage.passwordInput.fill('firstpassword');

    await signUpPage.passwordInput.blur();

    const weakPasswordError = page.getByText('Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character');

    await expect(weakPasswordError).toBeVisible();
})