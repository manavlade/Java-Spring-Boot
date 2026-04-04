import { Routes } from '@angular/router';
import { Upload } from './components/upload/upload';
import { Dashboard } from './components/dashboard/dashboard';
import { Home } from './components/home/home';
import { Graphsnew } from './graphsnew/graphsnew';
import { Login } from './login/login';
import { Signup } from './signup/signup';
import { Grapht } from './grapht/grapht';

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
    },
    {
        path: 'grapht',
        component: Grapht
    },
    {
        path: 'graph',
        component: Graphsnew
    },
    {
        path: 'login',
        component: Login
    }, 
    {
        path: 'signup',
        component: Signup
    },
];

