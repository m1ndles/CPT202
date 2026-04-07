-- ============================================================
-- Seed script for heritage resource data (English translation)
-- Run after schema.sql has been applied
-- ============================================================

-- Resource 1: Humble Administrator's Garden
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Humble Administrator''s Garden', 'Humble Administrator''s Garden', 'Classical Garden', 'Suzhou',
 'Built during the early 16th century in the Ming Dynasty, the Humble Administrator''s Garden is the largest classical garden in Suzhou and a masterpiece of Jiangnan garden design. It was inscribed as a UNESCO World Heritage Site in 1997. The garden is centred on water, with winding streams, elegant pavilions, and lush vegetation reflecting the distinctive character of the Jiangnan water region. The layout is divided into three sections — east, central, and west — with the central part anchored by the Distant Fragrance Hall, where water covers roughly three-fifths of the area.',
 'https://picsum.photos/seed/humble-garden/800/500', 'Text under CC BY-SA 4.0; images from Wikimedia Commons', 'APPROVED', 3820, '2024-02-10');
SET @r1 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r1, 'UNESCO World Heritage'), (@r1, 'Classical Garden'), (@r1, 'Ming Dynasty'), (@r1, 'Four Great Gardens of Suzhou');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r1, 'Garden Layout Map', 'image', 'https://picsum.photos/seed/humble-garden-map/600/400'),
(@r1, 'Distant Fragrance Hall Panorama', 'image', 'https://picsum.photos/seed/humble-garden-2/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r1, 'UNESCO World Heritage Entry', 'https://whc.unesco.org/en/list/813'),
(@r1, 'Official Website', 'https://www.szgarden.com.cn');

-- Resource 2: Lingering Garden
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Lingering Garden', 'Lingering Garden', 'Classical Garden', 'Suzhou',
 'Originally built in 1593 during the Ming Dynasty and renovated in the Qing Dynasty, the Lingering Garden is renowned for its masterful handling of architectural space. The Crown Cloud Peak is one of the largest standalone Lake Tai rocks in the Jiangnan region. The garden is divided into four sections — central, east, north, and west — each with a distinct character. It was inscribed as a UNESCO World Heritage Site in 1997 alongside other Suzhou classical gardens.',
 'https://picsum.photos/seed/lingering-garden/800/500', 'Text under CC BY-SA 4.0; images from Wikimedia Commons', 'APPROVED', 2640, '2024-02-12');
SET @r2 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r2, 'UNESCO World Heritage'), (@r2, 'Classical Garden'), (@r2, 'Ming Dynasty'), (@r2, 'Four Great Gardens of Suzhou'), (@r2, 'Lake Tai Rock');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r2, 'Crown Cloud Peak', 'image', 'https://picsum.photos/seed/lingering-2/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r2, 'UNESCO World Heritage Entry', 'https://whc.unesco.org/en/list/813');

-- Resource 3: Master of the Nets Garden
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Master of the Nets Garden', 'Master of the Nets Garden', 'Classical Garden', 'Suzhou',
 'First built around 1174 during the Southern Song Dynasty, the Master of the Nets Garden is one of the smallest yet most refined gardens in Suzhou. Covering roughly half a hectare, it radiates outward from a central pond and is celebrated for its ingenious spatial design. The Late Spring Studio courtyard was replicated as the Astor Court at the Metropolitan Museum of Art in New York. It became a UNESCO World Heritage Site in 1997.',
 'https://picsum.photos/seed/master-nets/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 1950, '2024-02-15');
SET @r3 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r3, 'UNESCO World Heritage'), (@r3, 'Classical Garden'), (@r3, 'Song Dynasty'), (@r3, 'Compact Garden');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r3, 'Moon Arrives and Wind Comes Pavilion', 'image', 'https://picsum.photos/seed/master-nets-2/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r3, 'UNESCO World Heritage Entry', 'https://whc.unesco.org/en/list/813');

-- Resource 4: Lion Grove Garden
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Lion Grove Garden', 'Lion Grove Garden', 'Classical Garden', 'Suzhou',
 'Built in 1342 during the Yuan Dynasty by the monk Tianru, the Lion Grove Garden is famous for its elaborate rockery. The formations are constructed from Lake Tai stones with winding caves and passages among stones that resemble crouching lions. Emperor Qianlong visited the garden on each of his six southern tours and commissioned replicas in the Old Summer Palace and the Chengde Mountain Resort. It was inscribed as a UNESCO World Heritage Site in 1997.',
 'https://picsum.photos/seed/lion-grove/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 2100, '2024-02-18');
SET @r4 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r4, 'UNESCO World Heritage'), (@r4, 'Classical Garden'), (@r4, 'Yuan Dynasty'), (@r4, 'Rockery'), (@r4, 'Lake Tai Stone');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r4, 'Rockery Panorama', 'image', 'https://picsum.photos/seed/lion-grove-2/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r4, 'UNESCO World Heritage Entry', 'https://whc.unesco.org/en/list/813');

-- Resource 5: Kunqu Opera
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Kunqu Opera', 'Kunqu Opera', 'Intangible Cultural Heritage', 'Suzhou',
 'Kunqu Opera originated in the Kunshan area of Suzhou between the 14th and 17th centuries and is one of the oldest surviving forms of Chinese opera. In 2001 UNESCO proclaimed it a Masterpiece of the Oral and Intangible Heritage of Humanity. It is known for its delicate vocal style, graceful integration of singing and movement, and highly literary scripts. Representative works include The Peony Pavilion, The Palace of Eternal Life, and The Peach Blossom Fan.',
 'https://picsum.photos/seed/kunqu-opera/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 4230, '2024-03-01');
SET @r5 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r5, 'UNESCO Intangible Heritage'), (@r5, 'Opera'), (@r5, 'Performing Arts'), (@r5, 'Kunshan');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r5, 'Kunqu Performance', 'image', 'https://picsum.photos/seed/kunqu-2/600/400'),
(@r5, 'Kunqu Costume Display', 'image', 'https://picsum.photos/seed/kunqu-3/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r5, 'UNESCO Intangible Heritage Entry', 'https://ich.unesco.org/en/RL/kunqu-opera-00004');

-- Resource 6: Suzhou Embroidery
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Suzhou Embroidery', 'Suzhou Embroidery', 'Traditional Craft', 'Suzhou',
 'Suzhou embroidery has a history of over two thousand years and is counted among the Four Great Embroideries of China. It is prized for its fineness, elegance, and lifelike imagery. The technique of double-sided embroidery — producing identical patterns on both faces with all thread ends hidden between layers — represents its highest achievement. It was inscribed on the National Intangible Cultural Heritage list in 2006.',
 'https://picsum.photos/seed/suzhou-embroidery/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 2870, '2024-03-05');
SET @r6 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r6, 'Intangible Heritage'), (@r6, 'Embroidery'), (@r6, 'Four Great Embroideries'), (@r6, 'Double-Sided Embroidery');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r6, 'Double-Sided Cat Embroidery', 'image', 'https://picsum.photos/seed/embroidery-2/600/400'),
(@r6, 'Embroidery Process', 'image', 'https://picsum.photos/seed/embroidery-3/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r6, 'China Intangible Heritage Network', 'https://www.ihchina.cn');

-- Resource 7: Nanjing Yunjin Brocade
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Nanjing Yunjin Brocade', 'Nanjing Yunjin Brocade', 'Traditional Craft', 'Nanjing',
 'Nanjing Yunjin brocade is a traditional silk craft with over 1,600 years of history. Its name means cloud brocade, inspired by brilliant colours resembling clouds at sunset. Weaving requires a traditional drawloom operated by two artisans working entirely from memory without computer assistance, a technique often described as a living fossil. UNESCO inscribed it on the Representative List of the Intangible Cultural Heritage of Humanity in 2009.',
 'https://picsum.photos/seed/nanjing-brocade/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 3150, '2024-03-08');
SET @r7 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r7, 'UNESCO Intangible Heritage'), (@r7, 'Silk Weaving'), (@r7, 'Brocade'), (@r7, 'Drawloom');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r7, 'Drawloom Demonstration', 'image', 'https://picsum.photos/seed/brocade-2/600/400'),
(@r7, 'Finished Brocade Display', 'image', 'https://picsum.photos/seed/brocade-3/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r7, 'UNESCO Intangible Heritage Entry', 'https://ich.unesco.org/en/RL/nanjing-yunjin-brocade-00200');

-- Resource 8: Suzhou Pingtan
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Suzhou Pingtan', 'Suzhou Pingtan', 'Intangible Cultural Heritage', 'Suzhou',
 'Suzhou Pingtan is a collective term for Suzhou storytelling (pinghua) and ballad singing (tanci), popular across the Jiangsu, Zhejiang, and Shanghai region for over 400 years. Pinghua emphasises spoken narration while tanci combines narration with singing accompanied by the pipa or sanxian, performed in the Wu dialect. Classic repertoires include Romance of the Three Kingdoms, Water Margin, and Legend of the White Snake. It was inscribed on the National Intangible Cultural Heritage list in 2006.',
 'https://picsum.photos/seed/suzhou-pingtan/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 1680, '2024-03-10');
SET @r8 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r8, 'Intangible Heritage'), (@r8, 'Wu Dialect'), (@r8, 'Storytelling'), (@r8, 'Ballad Singing');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r8, 'Live Pingtan Performance', 'image', 'https://picsum.photos/seed/pingtan-2/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r8, 'China Intangible Heritage Network', 'https://www.ihchina.cn');

-- Resource 9: Nanjing Ming City Wall
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Nanjing Ming City Wall', 'Nanjing Ming City Wall', 'Historic Architecture', 'Nanjing',
 'The Nanjing Ming City Wall was constructed between 1369 and 1393 during the reign of Emperor Hongwu. It is the longest and best-preserved brick city wall in the world, with an original perimeter of approximately 35.3 kilometres. Every brick carries inscriptions recording the place of manufacture and the names of the craftsmen responsible, reflecting rigorous quality control in ancient engineering. About 25 kilometres of the wall survive today, standing as one of the city''s most important historic landmarks.',
 'https://picsum.photos/seed/nanjing-wall/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 2980, '2024-03-12');
SET @r9 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r9, 'Historic Architecture'), (@r9, 'Ming Dynasty'), (@r9, 'City Wall'), (@r9, 'World Record'), (@r9, 'Nanjing');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r9, 'City Wall Panorama', 'image', 'https://picsum.photos/seed/nanjing-wall-2/600/400'),
(@r9, 'Brick Inscriptions', 'image', 'https://picsum.photos/seed/nanjing-wall-3/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r9, 'Nanjing City Wall Conservation Centre', 'http://www.njcitywall.com');

-- Resource 10: Zhouzhuang Ancient Town
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Zhouzhuang Ancient Town', 'Zhouzhuang Ancient Town', 'Ancient Water Town', 'Suzhou',
 'Zhouzhuang is located in Kunshan, Suzhou and was founded during the Northern Song Dynasty over 900 years ago. It is often called the foremost water town of China. The town is surrounded by water on all sides, with canals running through its streets and 14 distinctive ancient bridges. The Twin Bridges — Shide Bridge and Yongan Bridge — became internationally known through the oil paintings of Chen Yifei. Over a hundred Ming and Qing dynasty residences remain intact.',
 'https://picsum.photos/seed/zhouzhuang/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 5120, '2024-03-15');
SET @r10 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r10, 'Ancient Town'), (@r10, 'Water Town'), (@r10, 'Song Dynasty'), (@r10, 'Jiangnan Residence'), (@r10, 'Twin Bridges');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r10, 'Twin Bridges Panorama', 'image', 'https://picsum.photos/seed/zhouzhuang-2/600/400'),
(@r10, 'Canal Scenery', 'image', 'https://picsum.photos/seed/zhouzhuang-3/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r10, 'Zhouzhuang Scenic Area', 'https://www.zhouzhuang.net');

-- Resource 11: Tongli Ancient Town
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Tongli Ancient Town', 'Tongli Ancient Town', 'Ancient Water Town', 'Suzhou',
 'Tongli is located in the Wujiang district of Suzhou and was founded during the Song Dynasty over 1,000 years ago. The town is embraced by 15 lakes and connected by 49 ancient bridges. It preserves a large number of Ming and Qing dynasty residences, and the classical garden Tuisi Garden within the town was inscribed as a UNESCO World Heritage Site in 1997 alongside the Suzhou classical gardens. Tongli is known for its abundance of bridges, lanes, and historic houses.',
 'https://picsum.photos/seed/tongli/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 3360, '2024-03-18');
SET @r11 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r11, 'Ancient Town'), (@r11, 'Water Town'), (@r11, 'Song Dynasty'), (@r11, 'Tuisi Garden'), (@r11, 'UNESCO World Heritage');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r11, 'Tuisi Garden Interior', 'image', 'https://picsum.photos/seed/tongli-2/600/400'),
(@r11, 'Ancient Town Canal', 'image', 'https://picsum.photos/seed/tongli-3/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r11, 'Tongli Scenic Area', 'https://www.tongli.net');

-- Resource 12: Slender West Lake
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Slender West Lake', 'Slender West Lake', 'Natural and Cultural Landscape', 'Yangzhou',
 'Slender West Lake is located in the north-western suburbs of Yangzhou. Its name derives from the narrow, elongated shape of the lake. The landscape took shape during the Sui and Tang dynasties and was developed through the Song, Yuan, Ming, and Qing periods into a distinctive linear scenic area. The Twenty-Four Bridge is its most celebrated landmark, immortalised by the Tang poet Du Mu in the verse: On the moonlit night of the Twenty-Four Bridge, where does the fair lady teach the art of flute?',
 'https://picsum.photos/seed/slender-west-lake/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 4780, '2024-03-20');
SET @r12 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r12, 'Natural Landscape'), (@r12, 'Canal'), (@r12, 'Tang Dynasty'), (@r12, 'Twenty-Four Bridge'), (@r12, 'Yangzhou');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r12, 'Twenty-Four Bridge', 'image', 'https://picsum.photos/seed/west-lake-2/600/400'),
(@r12, 'Five Pavilion Bridge', 'image', 'https://picsum.photos/seed/west-lake-3/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r12, 'Slender West Lake Scenic Area', 'https://www.shouxihu.com');

-- Resource 13: Grand Canal of China (Yangzhou Section)
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Grand Canal of China — Yangzhou Section', 'Grand Canal of China (Yangzhou Section)', 'Grand Canal Heritage', 'Yangzhou',
 'The Yangzhou section of the Grand Canal stretches approximately 143 kilometres and includes heritage sites such as the ancient Hangou Canal, the Yangzhou city moat, and the salt administration office ruins. Yangzhou is the origin city of the Grand Canal — in 486 BC King Fuchai of Wu ordered the excavation of the Hangou Canal to link the Yangtze and Huai rivers. The Grand Canal was inscribed as a UNESCO World Heritage Site in 2014, and the Yangzhou section is one of the best-preserved and historically most significant stretches.',
 'https://picsum.photos/seed/grand-canal/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 2450, '2024-03-22');
SET @r13 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r13, 'UNESCO World Heritage'), (@r13, 'Grand Canal'), (@r13, 'Hydraulic Engineering'), (@r13, 'Sui Dynasty');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r13, 'Ancient Canal Night View', 'image', 'https://picsum.photos/seed/canal-2/600/400'),
(@r13, 'Hangou Canal Ruins', 'image', 'https://picsum.photos/seed/canal-3/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r13, 'UNESCO World Heritage Entry', 'https://whc.unesco.org/en/list/1443');

-- Resource 14: Yixing Zisha Pottery
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Yixing Zisha Pottery', 'Yixing Zisha Pottery', 'Traditional Craft', 'Wuxi',
 'Yixing Zisha pottery originates from Yixing, Jiangsu and has a history of over 600 years. Zisha teapots are made from a distinctive local clay and are valued for their craftsmanship, rich variety of forms, and the way they combine utility with artistry. The clay comes in three varieties — purple, green, and red — and after firing produces a fine, breathable texture that preserves the aroma of tea. Yixing Zisha pottery was inscribed on the National Intangible Cultural Heritage list in 2006.',
 'https://picsum.photos/seed/yixing-pottery/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 3690, '2024-03-25');
SET @r14 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r14, 'Intangible Heritage'), (@r14, 'Zisha'), (@r14, 'Pottery'), (@r14, 'Tea Culture'), (@r14, 'Handicraft');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r14, 'Zisha Teapot Collection', 'image', 'https://picsum.photos/seed/pottery-2/600/400'),
(@r14, 'Teapot Crafting Process', 'image', 'https://picsum.photos/seed/pottery-3/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r14, 'Yixing Zisha Museum', 'https://www.yxzisha.com');

-- Resource 15: Tiger Hill, Suzhou
INSERT INTO heritage_resources (title, title_en, category, place, description, thumbnail, copyright, status, view_count, created_at) VALUES
('Tiger Hill', 'Tiger Hill, Suzhou', 'Historic Architecture', 'Suzhou',
 'Tiger Hill is located in the north-west of Suzhou, rising 34.3 metres above sea level and covering about 20 hectares. It has been called the foremost scenic spot in the Wu region. Legend holds that King Helu of Wu was buried here during the Spring and Autumn period and that a tiger was seen crouching atop the hill, giving it its name. The Yunyan Temple Pagoda at the summit was built in 959 AD and leans approximately 3.59 degrees to the north-east, earning it the nickname the Leaning Tower of China. It is a nationally protected heritage site.',
 'https://picsum.photos/seed/tiger-hill/800/500', 'Text under CC BY-SA 4.0', 'APPROVED', 2760, '2024-03-28');
SET @r15 = LAST_INSERT_ID();
INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (@r15, 'Historic Architecture'), (@r15, 'Ancient Pagoda'), (@r15, 'Spring and Autumn Period'), (@r15, 'Leaning Tower'), (@r15, 'Suzhou Landmark');
INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES
(@r15, 'Tiger Hill Pagoda Panorama', 'image', 'https://picsum.photos/seed/tiger-hill-2/600/400'),
(@r15, 'Sword Testing Stone Pool', 'image', 'https://picsum.photos/seed/tiger-hill-3/600/400');
INSERT INTO heritage_resource_links (resource_id, label, url) VALUES
(@r15, 'Tiger Hill Scenic Area', 'https://www.tigerhill.com');
