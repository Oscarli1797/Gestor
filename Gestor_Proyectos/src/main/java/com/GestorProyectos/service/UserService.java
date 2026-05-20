package com.GestorProyectos.service;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.GestorProyectos.Constantes.KConstantes;
import com.GestorProyectos.Utils.RedisUtils;
import com.GestorProyectos.entity.Email;
import com.GestorProyectos.entity.User;
import com.GestorProyectos.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private EmailService emailService;

    // 600 segundos = 10 minutos
    private static final long VERIFY_EXPIRE_SECONDS = 600L;

    public User findByName(String name) {
        return userRepository.findByName(name);
    }

    public boolean isNameAvailable(String name) {
        return userRepository.findByName(name) == null;
    }

    public boolean isEmailAvailable(String email) {
        return userRepository.findByEmail(email) == null;
    }

    /**
     * Inicia el proceso de registro: genera un código, guarda el usuario
     * pendiente en Redis y envía el email de verificación.
     */
    public void initiateRegistration(String name, String password, String email) {
        String code = generateVerificationCode();
        User nuevoUsuario = new User(name, passwordEncoder.encode(password), email, "ROLE_USER");
        emailService.sendVerificationEmail(new Email(name, email, code));
        redisUtils.set(KConstantes.RedisConstantes.EMAILCODE + code, code, VERIFY_EXPIRE_SECONDS);
        redisUtils.set(KConstantes.RedisConstantes.NEWUSER + code, nuevoUsuario, VERIFY_EXPIRE_SECONDS);
    }

    /**
     * Verifica el código y, si es válido, persiste el usuario en la base de datos.
     * @return true si el usuario fue guardado correctamente
     */
    public boolean verifyAndSave(String code) {
        String storedCode = (String) redisUtils.get(KConstantes.RedisConstantes.EMAILCODE + code);
        if (storedCode == null || storedCode.isEmpty()) {
            return false;
        }
        Object userObj = redisUtils.get(KConstantes.RedisConstantes.NEWUSER + code);
        redisUtils.remove(KConstantes.RedisConstantes.EMAILCODE + code);
        redisUtils.remove(KConstantes.RedisConstantes.NEWUSER + code);

        if (userObj instanceof User nuevoUsuario) {
            userRepository.save(nuevoUsuario);
            return true;
        }
        return false;
    }

    private String generateVerificationCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(ThreadLocalRandom.current().nextInt(10));
        }
        return code.toString();
    }
}
