filePath=$(dirname $0)
cd $filePath/..

if [ $# -eq 0 ]; then
 DEPLOY=0
elif [ "$1" == "--deploy" ]; then
 DEPLOY=1 
else
 DEPLOY=0
fi

rm ./EC-Docker.jar

jar cvf ./EC-Docker.jar dsl/ META-INF/ pages/ lib/ htdocs/

if [ $DEPLOY -eq 1 ]; then
  echo "Installing plugin ..."
  ectool --server localhost login admin changeme
  ectool installPlugin ./EC-Docker.jar --force 1
  ectool promotePlugin EC-Docker-1.5.1
fi  