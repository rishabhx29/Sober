$modules = @("feature\home", "feature\apps", "feature\focus", "feature\stats", "feature\settings", "system\services", "system\overlay", "system\receivers")

foreach ($module in $modules) {
    $file = "C:\Rishabh\Sober\app\$module\build.gradle.kts"
    $content = Get-Content $file -Raw
    
    # Add plugins if not present
    if (-not ($content -match 'alias\(libs\.plugins\.kotlin\.compose\)')) {
        $content = $content -replace 'plugins \{', "plugins {`n    alias(libs.plugins.kotlin.compose)`n    alias(libs.plugins.hilt.plugin)`n    alias(libs.plugins.legacy.kapt)"
    }
    
    # Add buildFeatures compose
    if (-not ($content -match 'buildFeatures \{')) {
        $content = $content -replace 'compileOptions \{', "buildFeatures {`n        compose = true`n    }`n    compileOptions {"
    }

    # Add core dependencies
    if (-not ($content -match 'project\(":core:ui"\)')) {
        $content = $content -replace 'dependencies \{', "dependencies {`n    implementation(project(`":core:ui`"))`n    implementation(project(`":core:domain`"))`n    implementation(project(`":core:data`"))`n    implementation(platform(libs.androidx.compose.bom))`n    implementation(libs.androidx.compose.material3)`n    implementation(libs.androidx.compose.ui)`n    implementation(libs.androidx.compose.ui.tooling.preview)`n    implementation(libs.hilt.android)`n    kapt(libs.hilt.compiler)`n    implementation(libs.accompanist.drawablepainter)"
    }
    
    Set-Content -Path $file -Value $content
}
