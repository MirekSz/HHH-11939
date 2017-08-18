
package org.hibernate.bugs;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import sun.tools.jstat.Operator;

/**
 * This template demonstrates how to develop a standalone test case for Hibernate ORM. Although this is perfectly acceptable as a
 * reproducer, usage of ORMUnitTestCase is preferred!
 */
@TestForIssue(jiraKey = "HHH-11939")
public class ORMStandaloneTestCase {

	private SessionFactory sf;

	@Before
	public void setup() {
		StandardServiceRegistryBuilder srb = new StandardServiceRegistryBuilder().applySetting("hibernate.show_sql", "true")
				.applySetting("hibernate.format_sql", "true").applySetting("hibernate.hbm2ddl.auto", "create")
				.applySetting(AvailableSettings.STATEMENT_BATCH_SIZE, "10").applySetting("hibernate.order_inserts", "true");

		Metadata metadata = new MetadataSources(srb.build()).addAnnotatedClass(SaleDocument.class).addAnnotatedClass(SaleDocumentItem.class)
				.addAnnotatedClass(OperationRegistrySubject.class).addAnnotatedClass(Product.class).addAnnotatedClass(Operator.class)
				.buildMetadata();

		sf = metadata.buildSessionFactory();
	}

	@Test
	public void hhh11939Test() throws Exception {
		Session session = sf.openSession();

		Transaction beginTransaction = session.beginTransaction();
		SaleDocument saleDocument = new SaleDocument();
		session.persist(saleDocument);

		OperationRegistrySubject correctionSubject = new OperationRegistrySubject();
		session.persist(correctionSubject);

		session.flush(); // of course this provoke exception but it also could be some hql which flushes queue

		OperationRegistrySubject saleDocSubject = new OperationRegistrySubject();
		session.persist(saleDocSubject);

		SaleDocumentItem saleDocumentItem = new SaleDocumentItem();
		saleDocumentItem.setSaleDocument(saleDocument);
		saleDocumentItem.setSubject(saleDocSubject);
		session.persist(saleDocumentItem);

		SaleDocument correction = new SaleDocument();
		correction.setSubject(correctionSubject);
		session.persist(correction);

		beginTransaction.commit(); // FK_KEY VIOLATION, set
									// hibernate.order_inserts = false and works
	}

	@Test
	public void hhh11939Test2() throws Exception {
		Session session = sf.openSession();

		Transaction beginTransaction = session.beginTransaction();

		SaleDocument saleDocument = new SaleDocument();
		session.persist(saleDocument);

		OperationRegistrySubject correctionSubject = new OperationRegistrySubject();
		session.persist(correctionSubject);

		SaleDocumentItem saleDocumentItem = new SaleDocumentItem();
		saleDocumentItem.setSaleDocument(saleDocument);
		session.persist(saleDocumentItem);

		OperationRegistrySubject saleDocSubject = new OperationRegistrySubject();
		session.persist(saleDocSubject);

		SaleDocumentItem saleDocumentItem2 = new SaleDocumentItem();
		saleDocumentItem2.setSaleDocument(saleDocument);
		saleDocumentItem2.setSubject(saleDocSubject);
		session.persist(saleDocumentItem2);

		SaleDocument correction = new SaleDocument();
		correction.setSubject(correctionSubject);
		correction.setMain(saleDocumentItem);
		session.persist(correction);

		beginTransaction.commit(); // FK_KEY VIOLATION, set
									// hibernate.order_inserts = false and works
	}

	@Entity(name = "Product")
	public static class Product {

		@Id
		@GeneratedValue
		private Long id;
		@Column(unique = true)
		private String name;
		private String description;
		private Integer quantity;
		private BigDecimal price;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(final String description) {
			this.description = description;
		}

		public BigDecimal getPrice() {
			return price;
		}

		public void setPrice(final BigDecimal price) {
			this.price = price;
		}

		public Integer getQuantity() {
			return quantity;
		}

		public void setQuantity(final Integer quantity) {
			this.quantity = quantity;
		}
	}

	@Entity(name = "SaleDocument")
	public static class SaleDocument {

		@Id
		@GeneratedValue
		private Long id;
		private String number;
		@OneToMany(fetch = FetchType.LAZY, mappedBy = "saleDocument")
		private Set<SaleDocumentItem> items = new HashSet();
		@JoinColumn(name = "ID_SALE_DOCUMENT_CORRECTION", nullable = true)
		@ManyToOne(fetch = FetchType.LAZY)
		private SaleDocument corerctionSubject;
		private BigDecimal totalPrice;

		@JoinColumn(name = "ID_SUBJECT", nullable = true)
		@ManyToOne(fetch = FetchType.LAZY)
		private OperationRegistrySubject subject;

		@JoinColumn(name = "ID_SALE_DOCUMENT_ITEM", nullable = true)
		@ManyToOne(fetch = FetchType.LAZY)
		private SaleDocumentItem main;

		@JoinColumn(name = "ID_SALE_DOCUMENT", updatable = false)
		@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
		private final Set<SaleDocumentItem> itemSet = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(final String number) {
			this.number = number;
		}

		public Set<SaleDocumentItem> getItems() {
			return items;
		}

		public void setItems(final Set<SaleDocumentItem> items) {
			this.items = items;
		}

		public BigDecimal getTotalPrice() {
			return totalPrice;
		}

		public void setTotalPrice(final BigDecimal totalPrice) {
			this.totalPrice = totalPrice;
		}

		public void addItem(final SaleDocumentItem sdi) {
			this.getItems().add(sdi);
			sdi.setSaleDocument(this);
		}

		public SaleDocument getCorerctionSubject() {
			return corerctionSubject;
		}

		public void setCorerctionSubject(final SaleDocument corerctionSubject) {
			this.corerctionSubject = corerctionSubject;
		}

		public OperationRegistrySubject getSubject() {
			return subject;
		}

		public void setSubject(final OperationRegistrySubject subject) {
			this.subject = subject;
		}

		public SaleDocumentItem getMain() {
			return main;
		}

		public void setMain(final SaleDocumentItem main) {
			this.main = main;
		}

		public Set<SaleDocumentItem> getItemSet() {
			return itemSet;
		}
	}

	@Entity(name = "SaleDocumentItem")
	public class SaleDocumentItem {

		@Id
		@GeneratedValue
		private Long id;
		private Integer lp;
		@ManyToOne(optional = true)
		private Product product;
		@JoinColumn(name = "ID_SALE_DOCUMENT", nullable = true)
		@ManyToOne(fetch = FetchType.LAZY)
		private SaleDocument saleDocument;
		@JoinColumn(name = "ID_SUBJECT", nullable = true)
		@ManyToOne(fetch = FetchType.LAZY)
		private OperationRegistrySubject subject;
		private Integer quantity;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public Integer getLp() {
			return lp;
		}

		public void setLp(final Integer lp) {
			this.lp = lp;
		}

		public Product getProduct() {
			return product;
		}

		public void setProduct(final Product product) {
			this.product = product;
		}

		public Integer getQuantity() {
			return quantity;
		}

		public void setQuantity(final Integer quantity) {
			this.quantity = quantity;
		}

		public SaleDocument getSaleDocument() {
			return saleDocument;
		}

		public void setSaleDocument(final SaleDocument saleDocument) {
			this.saleDocument = saleDocument;
		}

		public OperationRegistrySubject getSubject() {
			return subject;
		}

		public void setSubject(final OperationRegistrySubject subject) {
			this.subject = subject;
		}

	}

	@Entity(name = "OperationRegistrySubject")
	public class OperationRegistrySubject {

		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

	}

}
