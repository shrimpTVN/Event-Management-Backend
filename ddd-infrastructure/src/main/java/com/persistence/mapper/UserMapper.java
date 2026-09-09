package com.persistence.mapper;

import com.model.User;
import com.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

// Thuộc tính componentModel = "spring" báo cho MapStruct biết
// phải tự động biến cái Mapper này thành một Spring Bean (@Component)
// để mình có thể dùng @Autowired hoặc Constructor Injection.
@Mapper(componentModel = "spring")
public interface UserMapper {

    // (Tùy chọn) Cung cấp một instance thủ công nếu bạn không dùng Spring dependency injection
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    /**
     * Map từ Domain Model sang JPA Entity
     */
    // Nếu tên các trường trùng khớp 100%, bạn không cần viết annotation @Mapping.
    // Dòng dưới đây là ví dụ nếu tên trường BỊ LỆCH.
    // @Mapping(source = "username", target = "userNameInDB")
    UserJpaEntity toEntity(User user);

    /**
     * Map ngược từ JPA Entity ra Domain Model
     */
    User toDomain(UserJpaEntity entity);
}