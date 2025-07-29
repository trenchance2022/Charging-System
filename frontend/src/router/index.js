import { createRouter, createWebHistory } from 'vue-router';

const routes = [
    // 登录注册界面
    { path: '/login', component: () => import('../views/LoginView.vue') },
    { path: '/register', component: () => import('../views/RegisterView.vue') },
    // 用户界面
    { path: '/user/console', component: () => import('../views/UserConsole.vue') },
    { path: '/user/ChargeRequest', component: () => import('../views/RegisterChargeRequest.vue') },
    { path: '/user/ModifyRequest', component: () => import('../views/ModifyRequest.vue') },
    // 账单界面
    {path: '/user/bill/list', name: 'BillList', component: () => import('../views/BillListView.vue')},
    {path: '/user/bill/:billId', name: 'BillDetail', component: () => import('../views/BillDetailView.vue')},

    // 管理员界面
    { path: '/admin/console', component: () => import('../AdminViews/AdminConsole.vue') },
    { path: '/admin/pile/:pileId', component: () => import('../AdminViews/ChargingPileDetail.vue')},
    { path: '/admin/settings', component: () => import('../AdminViews/SystemSettings.vue')},
    { path: '/admin/ChargingPileList', component: () => import('../AdminViews/ChargingPileList.vue')},
    { path: '/admin/report', component: () => import('../AdminViews/ReportView.vue')},
    

    // 默认重定向登录页
    { path: '/', redirect: '/login' },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;
