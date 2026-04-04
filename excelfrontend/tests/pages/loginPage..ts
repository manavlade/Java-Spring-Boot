
import { Locator, Page } from "@playwright/test";
import { environment } from "../../src/environments/environment";

export const loginURL = `${environment.Login_URL}`;

export const dashboardURL = `${environment.dashboard_URL}`;

export class LoginPage {
    readonly page: Page;
    readonly emailInput: Locator;
    readonly passwordInput: Locator
    readonly loginButton: Locator;

    constructor(page: Page) {
        this.page = page;
        this.emailInput = page.locator('#email');
        this.passwordInput = page.locator('#password');
        this.loginButton = page.getByRole('button', { name: 'Login' });
    }

    async goToLoginPage() {
        await this.page.goto(loginURL);
    }

    // async goToDashboardPage(){
    //     await this.page.goto(dashboardURL);
    // }

    async loginUser(emailInput: string, password: string) {
        await this.emailInput.fill(emailInput);
        await this.passwordInput.fill(password);
        await this.loginButton.click();
    }
}
