#!/usr/bin/env php
<?php
/**
 * CSS Minifier Tool - autumo beetRoot
 *
 * Minifies the uncompressed stylesheets of beetRoot
 * (web/css/uncompressed/*.css) into the files the pages load
 * (web/css/*.min.css). Taken over from autumo-psai-web (webroot/css/minify_css.php),
 * only the directories and the output names are adapted to beetRoot.
 *
 * Usage:
 *   php bin/minify-css.php             - Compress base.css and style.css
 *   php bin/minify-css.php --list      - List the files
 *   php bin/minify-css.php --help      - Show this help
 *   php bin/minify-css.php base.css    - Compress only the given files
 */

$cssDir = dirname(__DIR__) . '/web/css';
$srcDir = $cssDir . '/uncompressed';

// Only the stylesheets of this project; the third party files (foundation,
// jquery-ui, password-strength) are shipped minified already.
$defaultFiles = ['base.css', 'style.css', 'foundation.css', 'password-strength.css'];

function compressCss($file) {
    $content = file_get_contents($file);

    // Preserve first comment block (copyright header), remove others
    if (preg_match('/^\s*\/\*[\s\S]*?\*\//', $content, $match)) {
        $copyright = trim($match[0]);
        $content = preg_replace('/^\s*\/\*[\s\S]*?\*\//', '', $content);
        $content = trim($content);

        // Compress only the CSS part, keep copyright multi-line
        $content = preg_replace('!/\*[^*]*\*+([^/][^*]*\*+)*/!', '', $content);
        $content = preg_replace('/\s+/', ' ', $content);
        $content = preg_replace('/\s*([{}:;,])\s*/', '$1', $content);
        $content = preg_replace('/;}/', '}', $content);

        return $copyright . "\n\n" . $content;
    } else {
        // Remove all comments if no copyright header found
        $content = preg_replace('!/\*[^*]*\*+([^/][^*]*\*+)*/!', '', $content);
        $content = preg_replace('/\s+/', ' ', $content);
        $content = preg_replace('/\s*([{}:;,])\s*/', '$1', $content);
        $content = preg_replace('/;}/', '}', $content);
        return trim($content);
    }
}

function cssFiles($srcDir, $names) {
    $files = [];
    foreach ($names as $name) {
        $file = $srcDir . '/' . $name;
        if (is_file($file)) {
            $files[] = $file;
        } else {
            echo "Missing: $file\n";
        }
    }
    return $files;
}

// CLI handling
$opts = getopt('hl', ['help', 'list']);
$help = isset($opts['h']) || isset($opts['help']);
$list = isset($opts['l']) || isset($opts['list']);
$args = array_slice($argv, 1);
$names = $defaultFiles;
foreach ($args as $arg) {
    if ($arg[0] !== '-') {
        $names = $arg === '--all' ? array_map('basename', glob($srcDir . '/*.css')) : [$arg];
    }
}

if ($help || $list) {
    echo "CSS Minifier Tool - autumo beetRoot\n";
    echo "===================================\n\n";

    echo "Usage: php bin/minify-css.php [options] [file.css ...]\n\n";
    echo "Options:\n";
    echo "  -l, --list    List the files\n";
    echo "  -h, --help    Show this help\n\n";

    if ($list) {
        echo "Source (web/css/uncompressed/):\n";
        foreach (cssFiles($srcDir, array_map('basename', glob($srcDir . '/*.css'))) as $file) {
            echo '  ' . basename($file) . ' (' . number_format(filesize($file)) . " bytes)\n";
        }
        echo "\nTarget (web/css/):\n";
        foreach ($defaultFiles as $name) {
            $min = $cssDir . '/' . str_replace('.css', '.min.css', $name);
            $size = is_file($min) ? number_format(filesize($min)) . ' bytes' : 'not generated';
            echo '  ' . basename($min) . " ($size)\n";
        }
    }

    exit(0);
}

echo "CSS Minifier Tool - autumo beetRoot\n";
echo "===================================\n\n";

$totalOrig = 0;
$totalMin = 0;

echo "Compressing CSS files...\n";
echo "-----------------------\n\n";

foreach (cssFiles($srcDir, $names) as $file) {
    $minFile = $cssDir . '/' . str_replace('.css', '.min.css', basename($file));
    $compressed = compressCss($file);
    file_put_contents($minFile, $compressed);

    $origSize = filesize($file);
    $minSize = strlen($compressed);
    $totalOrig += $origSize;
    $totalMin += $minSize;

    $savings = $origSize > 0 ? round((1 - $minSize / $origSize) * 100, 1) : 0;

    echo '  ' . basename($file) . ' -> ' . basename($minFile) . "\n";
    echo '    ' . number_format($origSize) . ' -> ' . number_format($minSize) . ' bytes';
    if ($origSize > 0) {
        echo " (-$savings%)";
    }
    echo "\n";
}

echo "\n";
echo "Summary:\n";
echo "---------\n";
echo 'Files compressed: ' . count(cssFiles($srcDir, $names)) . "\n";
echo 'Total: ' . number_format($totalOrig) . ' -> ' . number_format($totalMin) . " bytes\n";
