// SPDX-License-Identifier: Apache-2.0

import { createApp } from 'vue'
import { createRouter } from './router'
import App from './App.vue'
import './styles.css'

createApp(App).use(createRouter()).mount('#app')

