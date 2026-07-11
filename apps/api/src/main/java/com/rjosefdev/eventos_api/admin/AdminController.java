package com.rjosefdev.eventos_api.admin;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/organizadores")
    public ResponseEntity<UsuarioAdminResponse> cadastrarOrganizador(
        @Valid @RequestBody CadastroOrganizadorRequest request
    ) {
        UsuarioAdminResponse organizador = adminService.cadastrarOrganizador(request);
        return ResponseEntity
            .created(URI.create("/admin/usuarios/" + organizador.id()))
            .body(organizador);
    }

    @GetMapping("/usuarios")
    public List<UsuarioAdminResponse> listarUsuarios() {
        return adminService.listarUsuarios();
    }
}
