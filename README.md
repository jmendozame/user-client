README - User Client Application
===============================

Este README proporciona instrucciones detalladas sobre cómo probar la aplicación de cliente de usuarios construida con Spring Boot. La aplicación utiliza Spring Security con OAuth2 y JWT para la autenticación y autorización.

Requisitos Previos
------------------

- **Java:** Asegúrate de tener Java instalado. La aplicación se construyó con Spring Boot, que requiere Java.
- **Cliente REST:** Puedes utilizar herramientas como Postman o curl para realizar solicitudes HTTP.

Configuración del Proyecto
--------------------------

1. **Clonar el Repositorio:** Clona este repositorio en tu máquina local.

    ```bash
    git clone https://github.com/jmendozame/user-client.git
    
    cd user-client
    ```

2. **Configurar la Base de Datos H2 (Opcional):** La aplicación está configurada para usar una base de datos H2 en memoria. No es necesario realizar ninguna configuración adicional para la base de datos, pero puedes acceder a la consola de H2 en http://localhost:8080/h2-console si deseas verificar los datos.

3. **Configuración de Propiedades:** En el archivo `src/main/resources/application.properties`, puedes ajustar la configuración de la base de datos, como la URL, el nombre de usuario y la contraseña.

Ejecutar la Aplicación
-----------------------

- **Compilar y Ejecutar:** Abre una terminal en el directorio del proyecto y ejecuta el siguiente comando para compilar y ejecutar la aplicación.

    ```bash
    ./mvnw spring-boot:run
   

    O bien, si estás en Windows:

    ```bash
    mvnw.cmd spring-boot:run
    ```

La aplicación se ejecutará en http://localhost:8080.

Probar las Funcionalidades
--------------------------

1. **Obtener Todos los Usuarios**
   - **Endpoint:** `GET http://localhost:8080/api/users`
   - **Descripción:** Obtiene todos los usuarios registrados.

2. **Crear un Nuevo Usuario**
   - **Endpoint:** `POST http://localhost:8080/api/users`
   - **Cuerpo de la Solicitud (Ejemplo):**
     ```json
		{
		    "nombre": "Nombre Usuario",
		    "correo": "correo@ejemplo.com",
		    "contraseña": "Contraseña123",
		    "telefonos": [
		        {
		            "numero": "123456789",
		            "codigoCiudad": "1",
		            "codigoPais": "+56"
		        }
		    ]
		}
     ```
     
   - **Descripción:** Crea un nuevo usuario. Asegúrate de proporcionar un correo electrónico único.

3. **Modificar un Usuario Existente**
   - **Endpoint:** `PUT http://localhost:8080/api/users/{id}`
   - **Cuerpo de la Solicitud (Ejemplo):**
     ```json
    {
          "id": 2,
          "nombre": "Jorge Luis Mendoza",
          "correo": "jorge@ejemplo.com",
          "contraseña": "$2a$10$Kr5iOM0LoB19gcXUIyPSFuSN3xovbGozEcUKiEe5CzLMzLwZCLN7G",
          "telefonos": [
              {
                  "numero": 954318921,
                  "codigoCiudad": 9,
                  "codigoPais": 56
              }
          ]
    }
     ```
   - **Descripción:** Modifica un usuario existente. Asegúrate de proporcionar el ID correcto.

4. **Actualizar Contraseña de un Usuario**
   - **Endpoint:** `PATCH http://localhost:8080/api/users/{id}/password`
   - **Cuerpo de la Solicitud (Ejemplo):**
     ```json
     {
       "nuevaContraseña": "S3cure!P@ssw0rd"
     }
     ```
   - **Descripción:** Actualiza la contraseña de un usuario por ID.

5. **Eliminar un Usuario (Lógicamente)**
   - **Endpoint:** `DELETE http://localhost:8080/api/users/{id}`
   - **Descripción:** Elimina lógicamente un usuario por ID.

Notas Adicionales
-----------------

- **Autenticación:** Para realizar operaciones que requieran autenticación, debes obtener un token de acceso OAuth2 utilizando la URL POST http://localhost:8080/oauth/token. Utiliza el usuario y la contraseña proporcionados en las instrucciones.

- **Formato de Contraseña:** La contraseña debe seguir el formato especificado en la propiedad `password.regex` en el archivo `application.properties`.

- **Swagger:** La documentación de la API se puede acceder en http://localhost:8080/swagger-ui.html.
