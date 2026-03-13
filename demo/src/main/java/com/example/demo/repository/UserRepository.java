package com.example.demo.repository;

// import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.models.User;

// import java.util.Optional;

// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Modifying;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
// import org.springframework.transaction.annotation.Transactional;

// import com.example.demo.models.User;

// public interface UserRepository extends JpaRepository<User, Long> {

//     @Modifying
//     @Transactional
//     @Query(value = "INSERT INTO users (email, password) values (:email, :password)", nativeQuery = true)
//     void createUser(@Param("email") String email,
//             @Param("password") String password
//     );

//     @Query(value = "SELECT * FROM users", nativeQuery = true)
//     List<User> getAllUsers();

//     @Query(value = "SELECT email FROM users WHERE id = :id", nativeQuery = true)
//     Optional<User> getUserByID(@Param("id") long id);

//     @Modifying
//     @Transactional
//     @Query(value = "UPDATE users SET email = :email, password = :password WHERE id = :id", nativeQuery = true)
//     void updateUser(@Param("id") long id,
//             @Param("email") String email,
//             @Param("password") String password
//     );

//     @Modifying  
//     @Transactional
//     @Query(value = "DELETE FROM users WHERE id = :id", nativeQuery = true)
//     void deleteUser(@Param("id") Long id);
// }

public interface UserRepository extends JpaRepository<User, Long>{

}
