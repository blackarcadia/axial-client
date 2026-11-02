const { merge } = require('webpack-merge');
const common = require('./webpack.common.js');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyPlugin = require('copy-webpack-plugin');

module.exports = merge(common, {
  mode: 'production',
  plugins: [
    new HtmlWebpackPlugin({
      template: './index.html',
      inject: false,
    }),
    new CopyPlugin({
      patterns: [
        { from: 'img', to: 'img', globOptions: { ignore: ['**/axial-landscape.png', '**/.gitkeep'] } },
        { from: 'css', to: 'css' },
        { from: 'servers.html', to: 'servers.html' },
        { from: 'servers', to: 'servers' },
        { from: 'news', to: 'news' },
        { from: 'store', to: 'store' },
        { from: 'js/store-core.js', to: 'js/store-core.js' },
        { from: 'js/store.js', to: 'js/store.js' },
        { from: 'news-feed.php', to: 'news-feed.php' },
        { from: 'download-counter.php', to: 'download-counter.php' },
        { from: 'online-count.php', to: 'online-count.php' },
        { from: 'js/news.js', to: 'js/news.js' },
        { from: 'node_modules/dompurify/dist/purify.min.js', to: 'js/vendor/purify.min.js' },
        { from: 'js/navigation.js', to: 'js/navigation.js' },
        { from: 'js/servers.js', to: 'js/servers.js' },
        { from: 'fonts', to: 'fonts' },
        { from: 'js/mod-menu-core.js', to: 'js/mod-menu-core.js' },
        { from: 'js/mod-menu.js', to: 'js/mod-menu.js' },
        { from: 'js/hero-motion.js', to: 'js/hero-motion.js' },
        { from: 'js/crosshair-core.js', to: 'js/crosshair-core.js' },
        { from: 'js/hud-core.js', to: 'js/hud-core.js' },
        { from: 'js/hud-editor.js', to: 'js/hud-editor.js' },
        { from: 'js/crosshair-editor.js', to: 'js/crosshair-editor.js' },
        { from: 'icon.svg', to: 'icon.svg' },
        { from: 'favicon.ico', to: 'favicon.ico' },
        { from: 'robots.txt', to: 'robots.txt' },
        { from: 'icon.png', to: 'icon.png' },
        { from: '404.html', to: '404.html' },
        { from: 'site.webmanifest', to: 'site.webmanifest' },
      ],
    }),
  ],
});
