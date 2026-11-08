const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8081,
    proxy: {
      '/auth': { target: 'http://localhost:8080', changeOrigin: true },
      '/users': { target: 'http://localhost:8080', changeOrigin: true },
      '/courses': { target: 'http://localhost:8080', changeOrigin: true },
      '/labs': { target: 'http://localhost:8080', changeOrigin: true },
      '/schedule': { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
})
