param(
    [Parameter(Mandatory=$true)][string]$JdkHome,
    [Parameter(Mandatory=$true)][string]$BuildTools,
    [Parameter(Mandatory=$true)][string]$AndroidJar,
    [string]$BuildDir = "$PSScriptRoot/.build",
    [string]$ApkPath = "$PSScriptRoot/SignalField.apk"
)
$ErrorActionPreference = 'Stop'
$JdkHome = (Resolve-Path -LiteralPath $JdkHome).Path
$BuildTools = (Resolve-Path -LiteralPath $BuildTools).Path
$AndroidJar = (Resolve-Path -LiteralPath $AndroidJar).Path
New-Item -ItemType Directory -Force $BuildDir | Out-Null
$BuildDir = (Resolve-Path -LiteralPath $BuildDir).Path
function Run-Native([string]$Program, [string[]]$Arguments) {
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program failed with exit code $LASTEXITCODE" }
}
foreach ($dir in @('classes','dex','generated')) { New-Item -ItemType Directory -Force "$BuildDir/$dir" | Out-Null }
$sources = @(Get-ChildItem "$PSScriptRoot/src" -Recurse -Filter '*.java' | ForEach-Object FullName)
Run-Native "$JdkHome/bin/javac.exe" (@('--release','8','-encoding','UTF-8','-classpath',$AndroidJar,'-d',"$BuildDir/classes") + $sources)
Run-Native "$JdkHome/bin/jar.exe" @('cf',"$BuildDir/classes.jar",'-C',"$BuildDir/classes",'.')
Run-Native "$JdkHome/bin/java.exe" @('-cp',"$BuildTools/lib/d8.jar",'com.android.tools.r8.D8','--lib',$AndroidJar,'--min-api','26','--output',"$BuildDir/dex","$BuildDir/classes.jar")
Run-Native "$BuildTools/aapt2.exe" @('compile','--dir',"$PSScriptRoot/res",'-o',"$BuildDir/resources.zip")
Run-Native "$BuildTools/aapt2.exe" @('link','-o',"$BuildDir/unsigned.apk",'-I',$AndroidJar,'--manifest',"$PSScriptRoot/AndroidManifest.xml",'-A',"$PSScriptRoot/assets","$BuildDir/resources.zip")
Push-Location "$BuildDir/dex"
try { Run-Native "$BuildTools/aapt.exe" @('add',"$BuildDir/unsigned.apk",'classes.dex') } finally { Pop-Location }
Run-Native "$BuildTools/zipalign.exe" @('-f','4',"$BuildDir/unsigned.apk","$BuildDir/aligned.apk")
$key = "$BuildDir/development.keystore"
if (-not (Test-Path -LiteralPath $key)) {
    Run-Native "$JdkHome/bin/keytool.exe" @('-genkeypair','-keystore',$key,'-storepass','android','-keypass','android','-alias','signalfield','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Signal Field Development')
}
Run-Native "$JdkHome/bin/java.exe" @('-jar',"$BuildTools/lib/apksigner.jar",'sign','--ks',$key,'--ks-pass','pass:android','--key-pass','pass:android','--ks-key-alias','signalfield','--out',$ApkPath,"$BuildDir/aligned.apk")
Run-Native "$JdkHome/bin/java.exe" @('-jar',"$BuildTools/lib/apksigner.jar",'verify','--verbose',$ApkPath)
Write-Output "Built APK: $ApkPath"
