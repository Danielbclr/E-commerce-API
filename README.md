# E-Commerce API
## Feature Roadmap

This roadmap outlines the planned features for the Spring Boot E-commerce API.

-   [x] **Database**
  - [x] Set up and configure a relational database (MySQL).
  - [x] Define database schema and relationships using JPA/Hibernate.

-   [x] **Product Management**
  - [x] Create: Add new products (name, description, price, category).
  - [x] Read: Retrieve single product details and lists of products.
  - [x] Update: Modify existing product details.
  - [x] Delete: Remove products.

-   [x] **User Management**
  - [x] Registration: Allow users to create accounts (username, password, email).
  - [x] Login: Authenticate users.
  - [x] Authorization: Implement role-based access control (e.g., ADMIN, CUSTOMER).

-   [ ] **Shopping Cart**
  - [ ] Add to Cart: Add products to a user's cart.
  - [ ] View Cart: Display the contents of the current user's cart.
  - [ ] Update Cart: Modify product quantities in the cart.
  - [ ] Remove from Cart: Remove items from the cart.

-   [ ] **Order Management**
  - [ ] Create Order: Place an order from the shopping cart contents.
  - [ ] View Order History: Allow users to list their past orders.
  - [ ] View Order Details: Show details of a specific order.

-   [x] **Security**
  - [x] Implement Spring Security for authentication and authorization.
  - [ ] Use JWT (JSON Web Tokens) for stateless session management.

-   [x] **Data Validation**
  - [x] Implement input validation for API requests.

-   [x] **Error Handling**
  - [ ] Implement consistent and informative error handling.
  - [ ] Custom Exception objects
  
-   [x] **API Documentation**
  - [x] Integrate Swagger/OpenAPI for API documentation.

-   [ ] **Testing**
  - [ ] Write Unit Tests.
  - [ ] Write Integration Tests.

-   [ ] **Caching**
  - [ ] Implement caching strategies to improve performance (e.g., for products, categories).

-   [ ] **Docker**
  - [ ] Add support for Docker

