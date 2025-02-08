import { createApp } from 'vue';
import App from './App.vue';
import router from './router';

// ✅ Import Bootstrap
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';
import 'bootstrap-icons/font/bootstrap-icons.css';

// ✅ Import Global Styles
import './styles/global.css';
import './styles/bootstrap-overrides.css';

const app = createApp(App);
app.use(router);
app.mount('#app');
