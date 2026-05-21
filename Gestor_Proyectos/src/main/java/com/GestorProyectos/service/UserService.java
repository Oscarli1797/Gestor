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

    /**
     * Sends a 6-digit reset code to the email if it belongs to an existing account.
     * Always returns true to avoid revealing whether an email is registered.
     */
    public boolean initiatePasswordReset(String email) {
        if (userRepository.findByEmail(email) == null) {
            return true; // silent — don't reveal account existence
        }
        String code = generateVerificationCode();
        redisUtils.set(KConstantes.RedisConstantes.RESETCODE + code, email, VERIFY_EXPIRE_SECONDS);
        emailService.sendPasswordResetEmail(email, code);
        return true;
    }

    /**
     * Validates the reset code and updates the password.
     * @return true if successful, false if code is invalid/expired.
     */
    public boolean resetPassword(String code, String newPassword) {
        String email = (String) redisUtils.get(KConstantes.RedisConstantes.RESETCODE + code);
        if (email == null || email.isBlank()) return false;

        User user = userRepository.findByEmail(email);
        if (user == null) return false;

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisUtils.remove(KConstantes.RedisConstantes.RESETCODE + code);
        return true;
    }

    private String generateVerificationCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(ThreadLocalRandom.current().nextInt(10));
        }
        return code.toString();
    }
}
