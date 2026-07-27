param([string]$Dir, [string]$Jar)

Set-Location -LiteralPath $Dir
java -jar "$Dir\target\$Jar" --server.port=8080 `
  "--spring.datasource.url=jdbc:mysql://localhost:3307/envios_paraguay_cms?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" `
  --spring.datasource.password=root `
  "--app.upload.dir=C:\Users\astur\Desktop\Envios_Paraguay_CMS\uploads" `
  --app.admin.password=admin123
