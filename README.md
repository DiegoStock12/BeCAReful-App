# BeCAReful App

## Estructura

- `data`: Clases relacionadas con la base de datos SQLite propia de la aplicación-
	- `BecarefulDbEntry` no es más que una serie de constantes para hacer más fácil y ordenado el acceso a cada una de las columnas de la base de datos. Estas constantes también son usadas para rellenar parámetros de Bundles e Intents.
	- `BecarefulDbHelper` es la clase principal de la base de datos, con su respectiva estructura y métodos de inicialización y 'destrucción'. El diseño es de tipo Singleton de manera que no se pueda instanciar más de una base de datos al mismo tiempo. De esta manera se previenen errores
- `notif`: Clases relacionadas con las notificaciones de la aplicación.
	- `NotificationService` es la clase donde se define el servicio receptor de mensajes procedentes del nodo RabbitMQ y que nos proporcionará datos en tiempo real acerca de las emergencias que se produzcan en el coche y que se representarán como una notifiación en el dispositivo.
	- `NotificationUtils` es la clase donde se declara el tipo de notificaciones que se quieren presentar al usuario y donde están los métodos necesarios para construirlas.
- `pojo`: Java Objects utilizados para convertir los JSON provenientes del servidor en objectos JAVA manejables por la aplicación
- `sync`: Clases relacionadas con la sincronización de la aplicación. 
	- `BecarefulSyncIntentService` es un intent service que maneja las peticiones de sincronización instantánea y de borrado de notificaciones
	- `BecarefulSyncTask` es donde se define la tarea que se debe ejecutar periódicamente para recuperar datos del servidor y presentarlos por pantalla en la aplicaicón
	- `BecarefulSyncUtils` es donde se declara el Job que facilitará la ejecución periódica del servicio de refresco, así como algunos métodos auxiliares para cargar datos de/en la base de datos
	- `DataUpdaterJobService` es donde se definen las tareas que tiene que hacer periódicamente la aplicación
- `utils`: Simples métodos de utilidad
- `default package`: Actividades visibles en la aplicación