package com.home.backend.entities.enums;

// o que um membro pode fazer numa casa; cada HomeRole define o seu conjunto
public enum HomePermission {
    VIEW,            // ver casa, membros, itens e lista
    EDIT_LISTS,      // criar/editar/apagar itens e mexer na lista de compras
    INVITE,          // convidar pessoas
    MANAGE_MEMBERS,  // remover outros membros
    MANAGE_HOME      // renomear/apagar a casa, cancelar convites de outros
}
