# Necesita la imagen de java8 para poder construir el proyecto
FROM java:8
# Añadir el jar del directorio al contenedor
ADD Gestor_proyectos-0.0.1-SNAPSHOT.jar /Gestor.jar
# crear un fichero mall-tiny-docker-file.jar
RUN bash -c 'touch /Gestor.jar'
# declarar que se ejecuta en puerta 8089
EXPOSE 8089
# definir el paquete jar que debe de ejecutar
ENTRYPOINT ["java", "-jar","/Gestor.jar"]
# Definir el nombre del mantenedor 
MAINTAINER Chengjian Li