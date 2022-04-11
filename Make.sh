#!/bin/sh

mypath=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )/

cd $mypath

function pause(){
 read -s -n 1 -p "Press any key to continue . . ."
 echo ""
}



while true; do
    read -p "Build docker images (y/N): " yn
    case $yn in
        [Yy]* ) { builddocker="y" ; break ; } ;;
        [Nn]* ) { builddocker="n" ; break ; } ;;
        * ) { builddocker="n" ; break ; } ;;
    esac
done

while true; do
    read -p "Build java packages (y/N): " yn
    case $yn in
        [Yy]* ) { mavenbuild="y" ; break ; } ;;
        [Nn]* ) { mavenbuild="n" ; break ; } ;;
        * ) { mavenbuild="n" ; break ; } ;;
    esac
done

if [ "$mavenbuild" == "y" ]; then
  echo Building HAM
  cd ham
  mvn clean install
  cd ..

  echo Building sample applications
  cd samples/sampleapp
  mvn clean install
  cd ..
  cd ..

fi

if [ "$builddocker" == "y" ]; then
    echo Building main docker images
	cd docker/images
	chmod 777 *.sh
	./ImagesBuild.sh
	cd .. 
	cd ..

	echo Building sampleapp docker images
	cd samples/sampleapp/docker_multi
	chmod 777 *.sh
	./ImagesBuild.sh
	cd ..
	cd ..
fi

pause
