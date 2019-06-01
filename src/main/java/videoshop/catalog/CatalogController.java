/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package videoshop.catalog;

import videoshop.catalog.Disc.DiscType;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.validation.Valid;

import javax.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;
import org.salespointframework.inventory.Inventory;
import org.salespointframework.inventory.InventoryItem;
import org.salespointframework.quantity.Quantity;
import org.salespointframework.time.BusinessTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
class CatalogController {

	private static final Quantity NONE = Quantity.of(0);

	private final VideoCatalog catalog;
	private final Inventory<InventoryItem> inventory;
	private final BusinessTime businessTime;

	CatalogController(VideoCatalog videoCatalog, Inventory<InventoryItem> inventory, BusinessTime businessTime) {

		this.catalog = videoCatalog;
		this.inventory = inventory;
		this.businessTime = businessTime;
	}

	@GetMapping("/dvds")
	String dvdCatalog(Model model) {
//		CatalogDataInitializer catalogDataInitializer = new CatalogDataInitializer(catalog);
//		catalogDataInitializer.updateDemo();
		model.addAttribute("catalog", catalog.findByType(DiscType.DVD));
		model.addAttribute("title", "catalog.dvd.title");

		return "catalog";
	}

	@GetMapping("/blurays")
	String blurayCatalog(Model model) {

		model.addAttribute("catalog", catalog.findByType(DiscType.BLURAY));
		model.addAttribute("title", "catalog.bluray.title");

		return "catalog";
	}

	// (｡◕‿◕｡)
	// Befindet sich die angesurfte Url in der Form /foo/5 statt /foo?bar=5 so muss man @PathVariable benutzen
	// Lektüre: http://spring.io/blog/2009/03/08/rest-in-spring-3-mvc/
	@GetMapping("/disc/{disc}")
	String detail(@PathVariable Disc disc, Model model) {

		Optional<InventoryItem> item = inventory.findByProductIdentifier(disc.getId());
		Quantity quantity = item.map(InventoryItem::getQuantity).orElse(NONE);

		model.addAttribute("disc", disc);
		model.addAttribute("quantity", quantity);
		model.addAttribute("orderable", quantity.isGreaterThan(NONE));

		return "detail";
	}

	// (｡◕‿◕｡)
	// Der Katalog bzw die Datenbank "weiß" nicht, dass die Disc mit einem Kommentar versehen wurde,
	// deswegen wird die update-Methode aufgerufen
	@PostMapping("/disc/{disc}/comments")
	public String comment(@PathVariable Disc disc, @Valid CommentAndRating payload) {

		disc.addComment(payload.toComment(businessTime.getTime()));
		
		catalog.save(disc);

		return "redirect:/disc/" + disc.getId();
	}

	/**
	 * Describes the payload to be expected to add a comment.
	 *
	 * @author Oliver Gierke
	 */
	interface CommentAndRating {

		@NotEmpty
		String getComment();

		@Range(min = 1, max = 5)
		int getRating();

		default Comment toComment(LocalDateTime time) {
			return new Comment(getComment(), getRating(), time);
		}
	}
}
