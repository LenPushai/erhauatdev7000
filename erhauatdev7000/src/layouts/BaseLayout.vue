<template>
  <div class="d-flex">
    <!-- Sidebar -->
    <Sidebar />

    <!-- Main Content -->
    <div class="content-wrapper">
      <Navbar />
      <div class="container mt-4">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script>
import Sidebar from '@/components/Sidebar.vue';
import Navbar from '@/components/Navbar.vue';

export default {
  components: {
    Sidebar,
    Navbar
  }
};
</script>

<style scoped>
.content-wrapper {
  flex: 1;
  margin-left: 250px; /* Sidebar width */
  min-height: 100vh;
  background-color: #f8f9fa;
}
</style>
