mvn clean package

cp target/kie-server-extension-example2-1.0.0-SNAPSHOT.jar RHPAM_HOME/standalone/deployments/kie-server.war/WEB-INF/lib/

curl -u username:password 'http://localhost:8080/kie-server/services/rest/server/containers/myRestApi/restapi?user=rhpamAdmin&status=Ready&pid=4&page=0&pageSize=10'