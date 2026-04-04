import { createRouter as _createRouter, createWebHistory, type Router } from 'vue-router'

import Intervention from '../pages/Intervention.vue'
import Overview from '../pages/Overview.vue'
import Synonym from '../pages/Synonym.vue'
import Entity from '../pages/Entity.vue'
import Token from '../pages/Token.vue'
import Publish from '../pages/Publish.vue'
import Audit from '../pages/Audit.vue'

export function createRouter(): Router {
  return _createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', redirect: '/overview' },
      { path: '/overview', name: 'overview', component: Overview },
      { path: '/intervention', name: 'intervention', component: Intervention },
      { path: '/synonym', name: 'synonym', component: Synonym },
      { path: '/entity', name: 'entity', component: Entity },
      { path: '/token', name: 'token', component: Token },
      { path: '/publish', name: 'publish', component: Publish },
      { path: '/audit', name: 'audit', component: Audit },
    ],
  })
}

