package com.example.api.service.impl;

import com.example.api.exception.AccountAndPasswordError;
import com.example.api.model.dto.LoginDto;
import com.example.api.model.entity.Admin;
import com.example.api.model.entity.LoginLog;
import com.example.api.repository.AdminRepository;
import com.example.api.repository.LoginLogRepository;
import com.example.api.service.AdminService;
import com.example.api.service.EmailService;
import com.example.api.utils.DataTimeUtil;
import com.example.api.utils.JwtTokenUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    @Resource
    private AdminRepository adminRepository;

    @Resource
    private EmailService emailService;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public Admin save(Admin admin) throws Exception {
        if (!StringUtils.hasText(admin.getEmail()) || admin.getEmail().length() < 8
                || !StringUtils.hasText(admin.getPassword()) || admin.getPassword().length() < 5) {
            throw new Exception("请求参数异常");
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setCreateAt(DataTimeUtil.getNowTimeString());
        return adminRepository.save(admin);
    }

    @Override
    public Admin findById(String id) {
        return adminRepository.findById(id).orElse(null);
    }

    @Override
    public boolean sendEmail(String email) throws Exception {
        Admin admin = adminRepository.findAdminByEmail(email);
        if (admin == null) throw new Exception("不存在的邮箱账户");
        return emailService.sendVerificationCode(email);
    }

    @Override
    public Admin loginByPassword(LoginDto dto) throws Exception {
        Admin admin = adminRepository.findFirstByEmail(dto.getEmail());
        if (admin == null || !StringUtils.hasText(admin.getPassword())
                || !StringUtils.hasText(dto.getPassword())) {
            throw new Exception("邮箱或密码错误");
        }

        String stored = admin.getPassword();
        boolean matched;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            matched = passwordEncoder.matches(dto.getPassword(), stored);
        } else {
            //兼容历史明文密码，登录成功后自动升级为BCrypt
            matched = stored.equals(dto.getPassword());
            if (matched) {
                admin.setPassword(passwordEncoder.encode(dto.getPassword()));
                adminRepository.save(admin);
            }
        }

        if (!matched) {
            throw new Exception("邮箱或密码错误");
        }
        return admin;
    }

    @Override
    public Admin loginByEmail(LoginDto dto) throws Exception {
        boolean status = emailService.checkVerificationCode(dto.getEmail(), dto.getCode());
        if (!status) throw new Exception("验证码错误");
        return adminRepository.findAdminByEmail(dto.getEmail());
    }

    @Override
    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    @Override
    public String createToken(Admin admin, long exp) {
        String rolesString = admin.getRoles();
        String[] roles = rolesString != null ? rolesString.split(";") : null;
        return JwtTokenUtil.createToken(admin.getEmail(), roles, exp);
    }

    @Override
    public void delete(String id) {
        adminRepository.deleteById(id);
    }

}
