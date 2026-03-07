# language: es
@HDU-01 @smoke @regression
Característica: Creación de recursos con semántica HTTP correcta

  Como consumidor de la API
  Quiero que las operaciones de creación respondan con 201 Created y el header Location
  Para poder acceder al recurso recién creado de forma estándar.

  Antecedentes:
    Dado que el catálogo de productos contiene al menos un producto activo con id 1
    Y que la mesa 5 se encuentra disponible

  @smoke
  Escenario: Crear una orden exitosamente retorna 201 con Location
    Dado que el payload de la orden es válido con tableId 5 y productId 1 con quantity 2
    Cuando se envía una solicitud POST a "/orders"
    Entonces la respuesta debe tener código 201
    Y debe incluir el header "Location" con el patrón "/orders/{uuid}"
    Y el campo "status" debe ser "PENDING"
    Y el header "Content-Type" debe ser "application/json"

  Escenario: La URL del header Location es accesible
    Dado que se creó una orden exitosamente vía POST a "/orders"
    Cuando se envía una solicitud GET a la URL del header "Location"
    Entonces la respuesta debe tener código 200
    Y el cuerpo debe contener la misma orden creada previamente

  Escenario: Creación fallida no incluye header Location
    Dado que el payload contiene un tableId inválido de 0
    Cuando se envía una solicitud POST a "/orders"
    Entonces la respuesta debe tener código 400
    Y no debe incluir el header "Location"
    Y el cuerpo debe seguir la estructura ErrorResponse
