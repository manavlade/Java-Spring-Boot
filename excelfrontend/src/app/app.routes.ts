import { Routes } from '@angular/router';
import { Upload } from './components/upload/upload';
import { Dashboard } from './components/dashboard/dashboard';
import { App } from './app';
import { Home } from './components/home/home';

export const routes: Routes = [
    {
        path: '',
        component: Home
    },
    {
        path: 'upload',
        component: Upload
    },
    {
        path: 'dashboard',
        component: Dashboard
    }
];

