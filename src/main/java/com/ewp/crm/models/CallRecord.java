package com.ewp.crm.models;

import javax.persistence.*;

@Entity
@Table (name = "call_client_info")
public class CallRecord {

	@Id
	@GeneratedValue
	@Column
	private Long id;

	@Column(name = "record_link")
	private String link;

	@ManyToOne
	private Client client;

	@OneToOne
	private ClientHistory clientHistory;

	public CallRecord() {}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public ClientHistory getClientHistory() {
		return clientHistory;
	}

	public void setClientHistory(ClientHistory clientHistory) {
		this.clientHistory = clientHistory;
	}
}