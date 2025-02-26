import Link from 'next/link';

export default function Home() {
  return (
    <div className="space-y-8">
      <section className="text-center py-12 bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-lg">
        <h1 className="text-4xl font-bold mb-4">Welcome to Our Online Store</h1>
        <p className="text-xl mb-6">Discover amazing products at great prices</p>
        <Link 
          href="/products" 
          className="bg-white text-blue-600 px-6 py-3 rounded-full font-semibold hover:bg-gray-100 transition-colors"
        >
          Shop Now
        </Link>
      </section>

      <section className="grid md:grid-cols-3 gap-8">
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-xl font-bold mb-4">Fast Shipping</h2>
          <p className="text-gray-600">Get your products delivered quickly and securely</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-xl font-bold mb-4">Quality Products</h2>
          <p className="text-gray-600">Curated selection of high-quality items</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h2 className="text-xl font-bold mb-4">24/7 Support</h2>
          <p className="text-gray-600">Always here to help with your questions</p>
        </div>
      </section>
    </div>
  );
}