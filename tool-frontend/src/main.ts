import './assets/main.css'
import '@vue-flow/controls/dist/style.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import Tooltip from 'primevue/tooltip'
import 'primeicons/primeicons.css'
import Aura from '@primeuix/themes/aura';

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(PrimeVue,{
    theme: {
        preset: Aura
    }
})
app.use(ToastService)
app.directive('tooltip', Tooltip)

app.mount('#app')
