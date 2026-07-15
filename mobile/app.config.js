// 동적 Expo 설정 — app.json 을 기본으로 받아, Firebase 설정 파일 경로만 EAS 파일 env 로 덮어쓴다.
//
// google-services.json / GoogleService-Info.plist 는 .gitignore 대상이라 EAS 클라우드 빌드에
// git 아카이브로 올라가지 않는다. 그래서 EAS "파일" 환경변수로 업로드하고, 빌드 시 주입되는
// 임시 경로(GOOGLE_SERVICES_JSON / GOOGLE_SERVICES_INFO_PLIST)로 googleServicesFile 을 가리킨다.
// 로컬(dev·로컬 빌드)에서는 env 가 없으므로 app.json 의 ./google-services.json 등을 그대로 쓴다.
module.exports = ({ config }) => {
  if (process.env.GOOGLE_SERVICES_JSON) {
    config.android = { ...config.android, googleServicesFile: process.env.GOOGLE_SERVICES_JSON };
  }
  if (process.env.GOOGLE_SERVICES_INFO_PLIST) {
    config.ios = { ...config.ios, googleServicesFile: process.env.GOOGLE_SERVICES_INFO_PLIST };
  }
  return config;
};
